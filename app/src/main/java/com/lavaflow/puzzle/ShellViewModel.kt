package com.lavaflow.puzzle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lavaflow.puzzle.core.*
import com.lavaflow.puzzle.data.ProfileStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BoosterSelection(val runId: String, val revision: Int, val booster: Booster, val first: Int? = null)
data class ShellState(val profile: PlayerProfile? = null, val busy: Boolean = false, val error: String? = null,
    val notice: String? = null, val selection: BoosterSelection? = null, val planning: Booster? = null,
    val achievementNotice: String? = null)

class ShellViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ProfileStore(application)
    private val mutable = MutableStateFlow(ShellState())
    val state = mutable.asStateFlow()
    private var loadJob: Job? = null
    private var boosterJob: Job? = null
    private var boosterGeneration = 0
    init { load() }

    fun load() {
        loadJob?.cancel()
        mutable.update { it.copy(error = null) }
        loadJob = viewModelScope.launch {
            try {
                store.initialize(BuildConfig.INITIAL_COINS)
                store.profiles.collect { profile -> mutable.update { previous ->
                    val added = previous.profile?.let { profile?.unlockedAchievements.orEmpty() - it.unlockedAchievements }.orEmpty()
                    previous.copy(profile = profile, error = null,
                        achievementNotice = added.takeIf { it.isNotEmpty() }?.joinToString(" · ") { name -> Achievement.valueOf(name).title } ?: previous.achievementNotice)
                } }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                mutable.update { it.copy(error = "Your progress could not be opened. Please try again.") }
            }
        }
    }

    fun buy(booster: Booster, feedback: () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                val result = store.purchase(booster, UUID.randomUUID().toString())
                mutable.update { it.copy(notice = when (result) {
                    PurchaseStatus.PURCHASED -> "${booster.title} added to your collection"
                    PurchaseStatus.INSUFFICIENT_COINS -> "Earn more coins by completing levels"
                    PurchaseStatus.INVENTORY_FULL -> "Your inventory is full"
                    PurchaseStatus.ALREADY_APPLIED -> "Purchase already saved"
                }) }
                if (result == PurchaseStatus.PURCHASED) feedback()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                mutable.update { it.copy(notice = "Purchase could not be saved. Your balance is unchanged.") }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }

    fun setting(setting: Setting, enabled: Boolean) {
        viewModelScope.launch {
            try { store.setting(setting, enabled) }
            catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                mutable.update { it.copy(notice = "Setting could not be saved. Please try again.") }
            }
        }
    }
    fun dismissNotice() { mutable.update { it.copy(notice = null) } }
    fun dismissAchievement() { mutable.update { it.copy(achievementNotice = null) } }

    private fun gameCommand(onSuccess: () -> Unit = {}, operation: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try { operation(); onSuccess() }
            catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                mutable.update { it.copy(notice = "Your progress could not be saved. Please try again.") }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }

    fun openLevel(level: Int = 1, opened: () -> Unit) {
        cancelBooster()
        gameCommand { if (store.openLevel(UUID.randomUUID().toString(), level)) opened() }
    }
    fun restartGame() {
        val round = mutable.value.profile?.activeRound ?: return
        cancelBooster()
        gameCommand { store.restart(UUID.randomUUID().toString(), round.runId) }
    }
    fun rotateTile(index: Int, feedback: () -> Unit) {
        val round = mutable.value.profile?.activeRound ?: return
        if (round.paused || round.phase != RoundPhase.PLAYING || index !in round.board.tiles.indices || !round.board.tiles[index].kind.rotatable) return
        gameCommand(feedback) { store.rotate(round.runId, index, round.revision) }
    }
    fun pauseGame(paused: Boolean) {
        if (paused) cancelBooster()
        if (mutable.value.profile?.activeRound == null) return
        // Lifecycle pauses must serialize behind any in-flight move, never be dropped by the UI busy guard.
        viewModelScope.launch {
            try { store.pause(paused) }
            catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                mutable.update { it.copy(notice = "Progress could not be saved. Please try again.") }
            }
        }
    }
    fun finishFlow(runId: String, revision: Int) = gameCommand { store.finishFlow(runId, revision) }

    fun claimDaily(feedback: () -> Unit) = gameCommand {
        val result = store.claimDaily()
        if (result == DailyStatus.CLAIMED) feedback()
        mutable.update { it.copy(notice = if (result == DailyStatus.CLAIMED)
            "Daily reward saved to your wallet and collection" else "Today's reward is already collected") }
    }

    fun markTutorial(step: String) {
        viewModelScope.launch {
            try { store.tutorialSeen(step) }
            catch (error: Exception) { if (error is kotlinx.coroutines.CancellationException) throw error }
        }
    }

    fun cancelBooster() {
        boosterGeneration++
        boosterJob?.cancel(); boosterJob = null
        mutable.update { it.copy(selection = null, busy = if (it.planning != null) false else it.busy, planning = null) }
    }

    fun selectBooster(booster: Booster, feedback: (FeedbackEvent) -> Unit) {
        val state = mutable.value
        val profile = state.profile ?: return
        val round = profile.activeRound ?: return
        if (state.busy || round.paused || round.phase != RoundPhase.PLAYING || profile.stock(booster) == 0) return
        if (state.selection?.booster == booster) { cancelBooster(); return }
        cancelBooster()
        feedback(FeedbackEvent.SELECT)
        val tutorialKey = "booster_${booster.name}"
        if (tutorialKey !in profile.tutorialSeen) {
            markTutorial(tutorialKey)
            mutable.update { it.copy(notice = when (booster) {
                Booster.HAMMER -> "Tap a rock to reveal a cross channel. Cancel is free."
                Booster.SWAP -> "Choose two neighboring channels. Cancel is free."
                Booster.HINT -> "Follow the glowing tiles. Their rotation is up to you."
                Booster.AUTO_ROTATE -> "One useful channel turns into place. No move used."
            }) }
        }
        if (booster == Booster.HAMMER || booster == Booster.SWAP) {
            mutable.update { it.copy(selection = BoosterSelection(round.runId, round.revision, booster)) }
        } else performBooster(BoosterRequest(round.runId, round.revision, booster), feedback)
    }

    fun tapTile(index: Int, feedback: (FeedbackEvent) -> Unit) {
        if (mutable.value.busy) return
        val round = mutable.value.profile?.activeRound ?: return
        val selection = mutable.value.selection
        if (selection == null) { rotateTile(index) { feedback(FeedbackEvent.ROTATE) }; return }
        if (round.paused || round.phase != RoundPhase.PLAYING || selection.runId != round.runId || selection.revision != round.revision) { cancelBooster(); return }
        if (index !in round.board.tiles.indices) return
        fun invalid(message: String) { feedback(FeedbackEvent.INVALID); mutable.update { it.copy(notice = message) } }
        if (selection.booster == Booster.HAMMER) {
            if (round.board.tiles[index].kind != TileKind.BLOCKED) { invalid("Hammer needs a blocked rock. Nothing spent."); return }
            performBooster(BoosterRequest(round.runId, round.revision, Booster.HAMMER, index), feedback)
        } else {
            if (!round.board.tiles[index].kind.playable) { invalid("Choose a channel, not a rock."); return }
            when {
                selection.first == null -> {
                    feedback(FeedbackEvent.SELECT)
                    mutable.update { it.copy(selection = selection.copy(first = index), notice = null) }
                }
                selection.first == index -> mutable.update { it.copy(selection = selection.copy(first = null)) }
                BoosterRules.swapped(round.board, selection.first, index) == null -> invalid("Choose a glowing adjacent tile. Nothing spent.")
                else -> performBooster(BoosterRequest(round.runId, round.revision, Booster.SWAP, selection.first, index), feedback)
            }
        }
    }

    private fun performBooster(request: BoosterRequest, feedback: (FeedbackEvent) -> Unit) {
        val round = mutable.value.profile?.activeRound ?: return
        val generation = ++boosterGeneration
        mutable.update { it.copy(busy = true, planning = request.booster) }
        boosterJob = viewModelScope.launch {
            try {
                val candidate = if (request.booster == Booster.SWAP) BoosterRules.swapped(round.board, request.first, request.second) else round.board
                val plan = if (request.booster == Booster.HAMMER || candidate == null) null else withContext(Dispatchers.Default) {
                    val context = currentCoroutineContext()
                    SolutionPlanner.solve(round.definition, candidate, cancelled = { !context.isActive })
                }
                val status = store.useBooster(request, plan)
                if (status == BoosterStatus.APPLIED) {
                    mutable.update { it.copy(selection = null, notice = null) }
                    feedback(if (request.booster == Booster.HAMMER) FeedbackEvent.HAMMER else FeedbackEvent.ROTATE)
                } else {
                    feedback(FeedbackEvent.INVALID)
                    mutable.update { it.copy(notice = when (status) {
                        BoosterStatus.NO_SOLUTION -> "No safe route found for that action. Nothing spent."
                        BoosterStatus.NO_EFFECT -> "No useful change here. Nothing spent."
                        BoosterStatus.EMPTY -> "No items left. Visit the Shop from Pause."
                        else -> "Choose another tile. Nothing spent."
                    }) }
                }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                mutable.update { it.copy(notice = "That action could not be saved. Please try again.") }
            } finally { if (generation == boosterGeneration) mutable.update { it.copy(busy = false, planning = null) } }
        }
    }
}
