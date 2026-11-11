package com.lavaflow.puzzle.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lavaflow.puzzle.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.lavaProfile by preferencesDataStore(name = "lava_profile")

/** Every mutation transforms the entire profile in one atomic DataStore transaction. */
class ProfileStore(private val dataStore: DataStore<Preferences>, private val today: () -> Long = { java.time.LocalDate.now().toEpochDay() }) {
    constructor(context: Context) : this(context.applicationContext.lavaProfile)
    private val key = stringPreferencesKey("profile_v1")
    val profiles: Flow<PlayerProfile?> = dataStore.data.map { it[key]?.let(ProfileCodec::decode) }

    suspend fun initialize(initialCoins: Long) {
        dataStore.edit { preferences ->
            val existing = preferences[key]
            if (existing == null) preferences[key] = ProfileCodec.encode(PlayerProfile(initialCoins))
            else {
                val profile = ProfileCodec.decode(existing) // Migrate v1 without resetting any progress.
                val round = profile.activeRound
                preferences[key] = ProfileCodec.encode(profile.copy(activeRound = round?.let {
                    if (it.phase == RoundPhase.PLAYING || it.phase == RoundPhase.FLOWING) it.copy(paused = true) else it
                }))
            }
        }
    }

    suspend fun purchase(booster: Booster, operationId: String): PurchaseStatus {
        var result = PurchaseStatus.INSUFFICIENT_COINS
        update { profile ->
            val decision = ProgressRules.purchase(profile, booster, operationId)
            result = decision.status
            decision.profile
        }
        return result
    }

    suspend fun setting(setting: Setting, enabled: Boolean) = update { it.withSetting(setting, enabled) }

    suspend fun claimDaily(): DailyStatus {
        var result = DailyStatus.ALREADY_CLAIMED
        update { profile ->
            val decision = DailyRewards.claim(profile, today())
            result = decision.status
            decision.profile
        }
        return result
    }

    suspend fun openLevel(runId: String, level: Int = 1): Boolean {
        var opened = false
        update { profile ->
            if (level !in GameCatalog.playableLevels || level > profile.unlockedLevel) profile else {
                opened = true
                val round = profile.activeRound
                profile.copy(activeRound = if (round == null || round.levelId != level || round.phase in setOf(RoundPhase.WON, RoundPhase.LOST))
                    PuzzleRound.start(runId, level) else round.copy(paused = true))
            }
        }
        return opened
    }

    suspend fun restart(runId: String, expectedRunId: String? = null) = update { profile ->
        val round = profile.activeRound
        if (round == null || expectedRunId != null && round.runId != expectedRunId) profile
        else profile.copy(activeRound = PuzzleRound.start(runId, round.levelId))
    }

    suspend fun pause(paused: Boolean) = update { profile -> profile.copy(activeRound = profile.activeRound?.let {
        if (it.phase == RoundPhase.PLAYING || it.phase == RoundPhase.FLOWING) it.copy(paused = paused) else it
    }) }

    suspend fun rotate(runId: String, index: Int, revision: Int) = update { profile ->
        val next = profile.activeRound?.let { round ->
            if (round.runId == runId) PuzzleRules.rotate(round, index, revision) else round
        }
        profile.copy(activeRound = next, tutorialSeen = if (next != profile.activeRound && next?.levelId == 1)
            profile.tutorialSeen + "rotate" else profile.tutorialSeen)
    }

    suspend fun tutorialSeen(step: String) = update { it.copy(tutorialSeen = it.tutorialSeen + step) }

    suspend fun useBooster(request: BoosterRequest, plan: SolutionPlan? = null): BoosterStatus {
        var result = BoosterStatus.NOT_READY
        update { profile ->
            val decision = BoosterRules.apply(profile, request, plan)
            result = decision.status
            decision.profile
        }
        return result
    }

    suspend fun finishFlow(runId: String, revision: Int) = update { ProgressRules.finishFlow(it, runId, revision) }

    private suspend fun update(transform: (PlayerProfile) -> PlayerProfile) {
        dataStore.edit { preferences ->
            val current = requireNotNull(preferences[key]) { "Profile is not initialized" }
            preferences[key] = ProfileCodec.encode(transform(ProfileCodec.decode(current)))
        }
    }
}

object ProfileCodec {
    fun encode(profile: PlayerProfile): String = JSONObject().apply {
        put("schemaVersion", 5)
        put("daily", JSONObject().apply {
            put("claims", profile.daily.claims)
            profile.daily.lastClaimDay?.let { put("lastClaimDay", it) }
        })
        put("coins", profile.coins)
        put("unlockedLevel", profile.unlockedLevel)
        put("inventory", JSONObject().apply { Booster.entries.forEach { put(it.name, profile.stock(it)) } })
        put("bestStars", JSONObject().apply { profile.bestStars.forEach { (level, stars) -> put(level.toString(), stars) } })
        put("bestMoves", JSONObject().apply { profile.bestMoves.forEach { (level, moves) -> put(level.toString(), moves) } })
        put("moveRecords", JSONObject().apply { profile.moveRecords.forEach { (key, moves) -> put(key, moves) } })
        put("rewardClaims", JSONObject().apply { profile.rewardClaims.forEach { (level, coins) -> put(level.toString(), coins) } })
        profile.activeRound?.let { round ->
            put("activeRound", JSONObject().apply {
                put("runId", round.runId); put("level", round.levelId); put("contentVersion", round.definition.contentVersion)
                put("turns", JSONArray(round.board.tiles.map { it.turns }))
                put("kinds", JSONArray(round.board.tiles.map { it.kind.name }))
                put("boostersUsed", JSONObject().apply { round.boostersUsed.forEach { (booster, count) -> put(booster.name, count) } })
                round.effect?.let { effect -> put("effect", JSONObject().apply {
                    put("booster", effect.booster.name); put("indices", JSONArray(effect.indices))
                }) }
                put("moves", round.moves); put("revision", round.revision); put("phase", round.phase.name)
                put("paused", round.paused); put("awardedCoins", round.awardedCoins)
                round.rewardBreakdown?.let { reward -> put("rewardBreakdown", JSONObject().apply {
                    put("level", reward.levelCoins); put("efficiency", reward.efficiencyCoins); put("firstClear", reward.firstClearCoins)
                }) }
            })
        }
        put("unassistedLevels", JSONArray(profile.unassistedLevels.sorted()))
        put("hammerUses", profile.hammerUses)
        put("hintUses", profile.hintUses)
        put("achievements", JSONArray(profile.unlockedAchievements.sorted()))
        put("completionEvents", JSONArray(profile.completionEvents.sorted()))
        put("boosterEvents", JSONArray(profile.boosterEvents.sorted()))
        put("recentPurchases", JSONArray(profile.recentPurchases))
        put("tutorialSeen", JSONArray(profile.tutorialSeen.sorted()))
        put("settings", JSONObject().apply {
            put("music", profile.settings.music); put("sound", profile.settings.sound); put("vibration", profile.settings.vibration)
        })
    }.toString()

    fun decode(encoded: String): PlayerProfile {
        val json = JSONObject(encoded)
        val version = json.getInt("schemaVersion")
        require(version in 1..5) { "Unsupported save version" }
        val stock = json.getJSONObject("inventory")
        val stars = json.getJSONObject("bestStars")
        val settings = json.getJSONObject("settings")
        val bestStars = stars.keys().asSequence().associate { it.toInt() to stars.getInt(it) }
        val savedMoves = json.optJSONObject("bestMoves")
        val savedClaims = json.optJSONObject("rewardClaims")
        val active = json.optJSONObject("activeRound")
        val round = active?.let {
            val levelId = it.getInt("level")
            val contentVersion = it.getInt("contentVersion")
            require(levelId in GameCatalog.playableLevels && contentVersion in 1..LevelCatalog.CONTENT_VERSION) { "Unsupported level content" }
            val turns = it.getJSONArray("turns")
            val original = LevelCatalog.get(levelId, contentVersion).board
            require(turns.length() == original.tiles.size)
            val kinds = if (version >= 3) it.getJSONArray("kinds") else null
            require(kinds == null || kinds.length() == original.tiles.size)
            val board = original.copy(tiles = original.tiles.mapIndexed { index, tile ->
                Tile(kinds?.getString(index)?.let(TileKind::valueOf) ?: tile.kind, turns.getInt(index))
            })
            val used = if (version >= 3) it.getJSONObject("boostersUsed") else null
            val effect = if (version >= 3) it.optJSONObject("effect") else null
            PuzzleRound(it.getString("runId"), board, it.getInt("moves"), it.getInt("revision"),
                RoundPhase.valueOf(it.getString("phase")), it.getBoolean("paused"), it.getLong("awardedCoins"), levelId,
                used?.keys()?.asSequence()?.associate { name -> Booster.valueOf(name) to used.getInt(name) } ?: emptyMap(),
                effect?.let { value -> BoosterEffect(Booster.valueOf(value.getString("booster")),
                    value.getJSONArray("indices").let { array -> (0 until array.length()).map(array::getInt) }) }, contentVersion,
                it.optJSONObject("rewardBreakdown")?.let { reward -> CompletionReward(reward.getLong("level"), reward.getLong("efficiency"), reward.getLong("firstClear")) })
        }
        fun strings(key: String) = json.getJSONArray(key).let { array -> (0 until array.length()).map { array.getString(it) } }
        return PlayerProfile(
            coins = json.getLong("coins"),
            daily = if (version >= 5) json.getJSONObject("daily").let { value ->
                DailyProgress(value.getLong("claims"), if (value.has("lastClaimDay")) value.getLong("lastClaimDay") else null)
            } else DailyProgress(),
            inventory = Booster.entries.associateWith { stock.getInt(it.name) },
            // Existing one-level saves advance to the newly available next level without granting coins.
            unlockedLevel = maxOf(json.getInt("unlockedLevel"), (bestStars.keys.maxOrNull()?.plus(1) ?: 1).coerceAtMost(GameCatalog.LEVEL_SLOTS)),
            bestStars = bestStars,
            bestMoves = savedMoves?.keys()?.asSequence()?.associate { it.toInt() to savedMoves.getInt(it) } ?: emptyMap(),
            rewardClaims = savedClaims?.keys()?.asSequence()?.associate { it.toInt() to savedClaims.getLong(it) }
                ?: bestStars.mapValues { PuzzleRules.reward(it.value) },
            activeRound = round,
            unassistedLevels = json.getJSONArray("unassistedLevels").let { array -> (0 until array.length()).map { array.getInt(it) }.toSet() },
            hammerUses = json.getInt("hammerUses"), hintUses = json.getInt("hintUses"),
            unlockedAchievements = strings("achievements").toSet(), completionEvents = strings("completionEvents").toSet(),
            boosterEvents = strings("boosterEvents").toSet(), recentPurchases = strings("recentPurchases"),
            settings = FeedbackSettings(settings.getBoolean("music"), settings.getBoolean("sound"), settings.getBoolean("vibration")),
            tutorialSeen = json.optJSONArray("tutorialSeen")?.let { array -> (0 until array.length()).map(array::getString).toSet() }
                ?: if (bestStars.isNotEmpty()) setOf("rotate", "connect") else emptySet(),
            moveRecords = if (version >= 4) json.getJSONObject("moveRecords").let { records ->
                records.keys().asSequence().associateWith { records.getInt(it) }
            } else savedMoves?.keys()?.asSequence()?.associate { "1:$it" to savedMoves.getInt(it) } ?: emptyMap()
        )
    }
}
