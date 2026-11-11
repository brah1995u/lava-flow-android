package com.lavaflow.puzzle

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lavaflow.puzzle.core.*
import com.lavaflow.puzzle.data.ProfileCodec
import com.lavaflow.puzzle.data.ProfileStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.io.File

/** Test APK only. All tests use a disposable DataStore, never the live player's profile. */
class GateInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val report = StringBuilder()
        var passed = 0
        fun pass(name: String) { passed++; report.append("PASS: $name\n"); sendStatus(0, Bundle().apply { putString("stream", "PASS: $name\n") }) }
        try {
            runBlocking {
                val file = File(targetContext.cacheDir, "gate-${System.nanoTime()}.preferences_pb")
                var job = SupervisorJob()
                fun open() = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
                var raw = open()
                var store = ProfileStore(raw)
                suspend fun profile() = requireNotNull(store.profiles.first())
                suspend fun rotate(index: Int) { val round = profile().activeRound!!; store.rotate(round.runId, index, round.revision) }
                suspend fun solve(extra: Int = 0) { repeat(extra) { rotate(4) }; FirstLevel.solutionTaps.forEach { rotate(it) } }
                suspend fun finish() { val round = profile().activeRound!!; store.finishFlow(round.runId, round.revision) }
                try {
                    store.initialize(2000)
                    check(profile() == PlayerProfile(2000))
                    store.initialize(99999)
                    check(profile().coins == 2000L)
                    pass("Clean save and initialization never refill existing currency")

                    coroutineScope { (1..20).map { async { store.purchase(Booster.HINT, "buy-$it") } }.awaitAll() }
                    check(profile().coins == 0L && profile().stock(Booster.HINT) == 20)
                    check(store.purchase(Booster.HAMMER, "no-funds") == PurchaseStatus.INSUFFICIENT_COINS)
                    check(store.purchase(Booster.HINT, "buy-1") == PurchaseStatus.ALREADY_APPLIED)
                    pass("Concurrent Shop transactions, insufficient funds and idempotence preserved")

                    store.setting(Setting.MUSIC, false)
                    store.setting(Setting.VIBRATION, false)
                    check(profile().settings == FeedbackSettings(false, true, false))
                    pass("Independent music, sound and vibration persist")

                    store.openLevel("first")
                    val initial = profile()
                    rotate(0)
                    check(profile() == initial)
                    FirstLevel.solutionTaps.take(3).forEach { rotate(it) }
                    check(profile().activeRound!!.connectivity.reachable.size == 5)
                    store.pause(true)
                    val paused = profile()
                    rotate(16)
                    check(profile() == paused)
                    store.pause(false)
                    finish()
                    check(profile().coins == 0L)
                    FirstLevel.solutionTaps.drop(3).forEach { rotate(it) }
                    check(profile().activeRound!!.phase == RoundPhase.FLOWING)
                    check(profile().bestStars.isEmpty())
                    pass("Persisted rotations, blocked rejection, pause and guarded flow before reward")

                    val flow = profile()
                    store.finishFlow("stale", flow.activeRound!!.revision)
                    check(profile() == flow)
                    store.pause(true)
                    finish()
                    check(profile().coins == 0L)
                    store.pause(false)
                    finish()
                    val won = profile()
                    check(won.coins == 184L && won.unlockedLevel == 2 && won.bestStars[1] == 3 && won.bestMoves[1] == 6)
                    check(won.rewardClaims[1] == 184L && won.activeRound!!.phase == RoundPhase.WON)
                    check(won.unlockedAchievements == setOf("FIRST_FLOW", "PERFECT_FLOW"))
                    finish()
                    check(profile() == won)
                    pass("Atomic real-solution completion records stars, moves, reward and achievements and unlocks Level 2")

                    store.restart("replay")
                    solve(4)
                    finish()
                    check(profile().coins == 252L && profile().activeRound!!.awardedCoins == 68L)
                    check(profile().bestMoves[1] == 6 && profile().bestStars[1] == 3)
                    check(profile().stock(Booster.HINT) == 20)
                    pass("Replay pays a new efficiency reward without degrading records or consuming boosters")

                    store.restart("lose")
                    repeat(12) { rotate(4) }
                    val lost = profile()
                    check(lost.activeRound!!.phase == RoundPhase.LOST && lost.coins == 252L)
                    rotate(7)
                    check(profile() == lost)
                    pass("Out-of-moves state persists and locks further rotations")

                    // A genuine 1-star clear followed by improvements, in the same isolated test file.
                    raw.edit { it[stringPreferencesKey("profile_v1")] = ProfileCodec.encode(PlayerProfile(0)) }
                    store.openLevel("bronze"); solve(4); finish()
                    check(profile().coins == 168L && profile().bestMoves[1] == 10)
                    store.restart("silver"); solve(2); finish()
                    check(profile().coins == 244L && profile().activeRound!!.awardedCoins == 76L)
                    store.restart("gold"); solve(); finish()
                    check(profile().coins == 328L && profile().activeRound!!.awardedCoins == 84L)
                    pass("Each clear pays; more saved moves pay more, with a first-clear premium only once")

                    val saved = profile()
                    check(ProfileCodec.decode(ProfileCodec.encode(saved)) == saved)
                    check(runCatching { ProfileCodec.decode("{\"schemaVersion\":999}") }.isFailure)
                    check(runCatching { ProfileCodec.decode("broken") }.isFailure)
                    pass("Schema 5 round-trip and malformed or future save rejection")

                    // Exercise the old shell's schema through the actual initialize migration transaction.
                    val legacy = JSONObject(ProfileCodec.encode(saved)).apply {
                        put("schemaVersion", 1); remove("bestMoves"); remove("rewardClaims"); remove("activeRound")
                    }.toString()
                    raw.edit { it[stringPreferencesKey("profile_v1")] = legacy }
                    store.initialize(2000)
                    check(profile().coins == 328L && profile().bestStars[1] == 3 && profile().rewardClaims[1] == 250L)
                    check(profile().bestMoves.isEmpty() && profile().activeRound == null)
                    check(raw.data.first()[stringPreferencesKey("profile_v1")]!!.contains("\"schemaVersion\":5"))
                    pass("Schema 1 migration preserves old progress and seeds reward entitlement without granting coins")

                    store.openLevel("restore")
                    rotate(7)
                    val playing = profile()
                    job.cancelAndJoin()
                    job = SupervisorJob()
                    raw = open()
                    store = ProfileStore(raw)
                    store.initialize(2000)
                    check(profile() == playing.copy(activeRound = playing.activeRound!!.copy(paused = true)))
                    check(file.length() > 0)
                    pass("Process-style store reopening restores the exact board and move count paused")

                    // Real old gameplay save migration, preserving its current attempt and earned money.
                    val oldRound = JSONObject(ProfileCodec.encode(profile())).apply {
                        put("schemaVersion", 2); remove("tutorialSeen"); put("unlockedLevel", 1)
                        getJSONObject("activeRound").apply { remove("kinds"); remove("boostersUsed"); remove("effect") }
                    }.toString()
                    raw.edit { it[stringPreferencesKey("profile_v1")] = oldRound }
                    store.initialize(2000)
                    check(profile().unlockedLevel == 2 && profile().coins == 328L)
                    check(profile().activeRound!!.board == playing.activeRound!!.board)
                    check(profile().activeRound!!.paused && profile().tutorialSeen.containsAll(setOf("rotate", "connect")))
                    pass("Schema 2 migration preserves the active board and earns no extra currency")

                    val fixture = PlayerProfile(1000, activeRound = PuzzleRound("boost-store"))
                    raw.edit { it[stringPreferencesKey("profile_v1")] = ProfileCodec.encode(fixture) }
                    Booster.entries.forEach { check(store.purchase(it, "real-${it.name}") == PurchaseStatus.PURCHASED) }
                    check(profile().coins == 300L && Booster.entries.all { profile().stock(it) == 1 })
                    pass("All four exact-price purchases persist one real gameplay item each")

                    var current = profile().activeRound!!
                    val hammer = BoosterRequest(current.runId, current.revision, Booster.HAMMER, 0)
                    check(store.useBooster(hammer.copy(first = 7)) == BoosterStatus.INVALID_TARGET)
                    val statuses = coroutineScope { (1..2).map { async { store.useBooster(hammer) } }.awaitAll() }
                    check(statuses.count { it == BoosterStatus.APPLIED } == 1)
                    check(profile().stock(Booster.HAMMER) == 0 && profile().activeRound!!.board.tiles[0].kind == TileKind.CROSS)
                    check(profile().activeRound!!.moves == 0 && profile().hammerUses == 1)
                    pass("Invalid Hammer costs nothing; concurrent duplicate Hammer atomically consumes exactly once")

                    current = profile().activeRound!!
                    val swapBoard = BoosterRules.swapped(current.board, 1, 6)!!
                    val swapPlan = SolutionPlanner.solve(current.definition, swapBoard)!!
                    check(store.useBooster(BoosterRequest(current.runId, current.revision, Booster.SWAP, 1, 6), swapPlan) == BoosterStatus.APPLIED)
                    current = profile().activeRound!!
                    check(store.useBooster(BoosterRequest(current.runId, current.revision, Booster.AUTO_ROTATE), SolutionPlanner.solve(current.definition, current.board)) == BoosterStatus.APPLIED)
                    current = profile().activeRound!!
                    check(store.useBooster(BoosterRequest(current.runId, current.revision, Booster.HINT), SolutionPlanner.solve(current.definition, current.board)) == BoosterStatus.APPLIED)
                    check(profile().inventory.values.all { it == 0 } && profile().activeRound!!.moves == 0)
                    check(profile().activeRound!!.effect!!.indices.size in 2..3)
                    pass("Swap, Auto Rotate and Hint consume inventory with current-board effects in one save")

                    store.tutorialSeen("booster_HAMMER")
                    val boosted = profile()
                    job.cancelAndJoin(); job = SupervisorJob(); raw = open(); store = ProfileStore(raw)
                    store.initialize(2000)
                    check(profile() == boosted.copy(activeRound = boosted.activeRound!!.copy(paused = true)))
                    pass("Boosted board, paid hint, inventory, assistance and tutorial survive process-style reopen")

                    check(!store.openLevel("locked", 30))
                    check(profile().activeRound!!.runId == boosted.activeRound!!.runId)
                    pass("Locked level opening is rejected by storage, not just the UI")

                    raw.edit { it[stringPreferencesKey("profile_v1")] = ProfileCodec.encode(PlayerProfile(0)) }
                    var expectedCampaign = 0L
                    for (authored in LevelCatalog.levels) {
                        val id = authored.definition.id
                        check(store.openLevel("campaign-$id", id))
                        authored.taps.forEach { rotate(it) }
                        finish()
                        check(profile().bestStars[id] == 3 && profile().unlockedLevel == minOf(id + 1, 30))
                        expectedCampaign += 160L + (id-1)*18L + (authored.definition.moveLimit-authored.taps.size)*(4L+(id-1)/10)
                        check(profile().coins == expectedCampaign)
                    }
                    check(profile().totalStars == 90 && profile().unlockedAchievements.containsAll(setOf("GRAND_FLOW", "PERFECTION", "LAVA_MASTER")))
                    val campaign = profile()
                    job.cancelAndJoin(); job = SupervisorJob(); raw = open(); store = ProfileStore(raw)
                    store.initialize(2000)
                    check(profile() == campaign)
                    pass("All 30 levels complete through persisted ordinary actions; 90 stars and campaign achievements survive reopen")

                    // A real edition-1 layout with a paid Hammer must remain readable and resumable.
                    val oldLevel=LevelCatalog.get(4,1)
                    var legacyRound=PuzzleRound("legacy-paid",oldLevel.board,levelId=4,contentVersion=1)
                    LegacyLevelCatalog.levels[3].taps.take(3).forEach { legacyRound=PuzzleRules.rotate(legacyRound,it,legacyRound.revision) }
                    var oldProfile=PlayerProfile(875,unlockedLevel=4,bestStars=mapOf(1 to 3),bestMoves=mapOf(1 to 6),
                        inventory=Booster.entries.associateWith { 2 },activeRound=legacyRound,settings=FeedbackSettings(false,true,false))
                    oldProfile=BoosterRules.apply(oldProfile,BoosterRequest(legacyRound.runId,legacyRound.revision,Booster.HAMMER,0)).profile
                    val oldEncoded=JSONObject(ProfileCodec.encode(oldProfile)).apply { put("schemaVersion",3); remove("moveRecords") }.toString()
                    raw.edit { it[stringPreferencesKey("profile_v1")]=oldEncoded }
                    store.initialize(2000)
                    check(profile()==oldProfile.copy(activeRound=oldProfile.activeRound!!.copy(paused=true),moveRecords=mapOf("1:1" to 6)))
                    check(profile().activeRound!!.definition.moveLimit==oldLevel.moveLimit)
                    check(profile().bestMovesFor(1,1)==6 && profile().bestMovesFor(1,2)==null)
                    pass("Schema 3 paid legacy attempt migrates with exact board, budget, inventory, wallet, settings and earlier move record")

                    check(store.openLevel("resume-legacy",4))
                    check(profile().activeRound!!.runId=="legacy-paid" && profile().activeRound!!.contentVersion==1)
                    store.restart("reviewed-four","legacy-paid")
                    check(profile().activeRound!!.contentVersion==3 && profile().activeRound!!.board==LevelCatalog.get(4).board)
                    check(profile().coins==875L && profile().stock(Booster.HAMMER)==1)
                    check(profile().bestStars[1]==3 && profile().bestMovesFor(1,1)==6)
                    pass("Explicit Restart adopts reviewed content while preserving spent inventory and all earned progress")

                    legacyRound=PuzzleRound("legacy-won",oldLevel.board,levelId=4,contentVersion=1)
                    LegacyLevelCatalog.levels[3].taps.forEach { legacyRound=PuzzleRules.rotate(legacyRound,it,legacyRound.revision) }
                    val oldWon=ProgressRules.finishFlow(PlayerProfile(100,unlockedLevel=4,activeRound=legacyRound),legacyRound.runId,legacyRound.revision)
                    val oldWonEncoded=JSONObject(ProfileCodec.encode(oldWon)).apply { put("schemaVersion",3); remove("moveRecords") }.toString()
                    raw.edit { it[stringPreferencesKey("profile_v1")]=oldWonEncoded }
                    store.initialize(99999)
                    check(profile().coins==oldWon.coins && profile().activeRound!!.phase==RoundPhase.WON)
                    check(profile().activeRound!!.board==oldWon.activeRound!!.board && profile().activeRound!!.contentVersion==1)
                    store.finishFlow(legacyRound.runId,legacyRound.revision)
                    check(profile().coins==oldWon.coins)
                    pass("A completed old level restores through its legacy layout and cannot grant a duplicate award")

                    val beforeVersioned=profile()
                    store.restart("edition2-record",legacyRound.runId)
                    LevelCatalog.levels[3].taps.forEach { rotate(it) }
                    finish()
                    check(profile().coins==beforeVersioned.coins+116L)
                    check(profile().bestMovesFor(4,1)==12 && profile().bestMovesFor(4,3)==10)
                    val versioned=profile()
                    job.cancelAndJoin(); job=SupervisorJob(); raw=open(); store=ProfileStore(raw)
                    store.initialize(2000)
                    check(profile()==versioned)
                    pass("New and earlier puzzle move records coexist and survive reopen with exactly one replay payment")

                    val editionTwo = LevelCatalog.get(4,2)
                    var retainedRound=PuzzleRound("edition-two-paid",editionTwo.board,levelId=4,contentVersion=2)
                    retainedRound=PuzzleRules.rotate(retainedRound,2,0)
                    val retained=PlayerProfile(432,unlockedLevel=4,activeRound=retainedRound,
                        bestStars=mapOf(1 to 3),moveRecords=mapOf("2:1" to 6),inventory=Booster.entries.associateWith { 2 })
                    val schemaFour=JSONObject(ProfileCodec.encode(retained)).apply { put("schemaVersion",4);remove("daily") }.toString()
                    raw.edit { it[stringPreferencesKey("profile_v1")]=schemaFour }
                    store.initialize(99999)
                    check(profile()==retained.copy(activeRound=retainedRound.copy(paused=true)))
                    check(profile().daily==DailyProgress() && profile().activeRound!!.definition.moveLimit==15)
                    pass("Schema 4 keeps edition-2 board, budget, money, inventory and records; daily starts unclaimed")

                    var today=20_000L
                    store=ProfileStore(raw) { today }
                    val beforeDaily=profile()
                    val claims=coroutineScope { (1..20).map { async { store.claimDaily() } }.awaitAll() }
                    check(claims.count { it==DailyStatus.CLAIMED }==1)
                    check(profile().coins==beforeDaily.coins+75 && profile().stock(Booster.HINT)==beforeDaily.stock(Booster.HINT)+1)
                    check(profile().activeRound==beforeDaily.activeRound && profile().daily==DailyProgress(1,today))
                    pass("Twenty concurrent daily taps credit day 1 coins and Hint exactly once without changing the active puzzle")
                    val claimed=profile()
                    job.cancelAndJoin();job=SupervisorJob();raw=open();store=ProfileStore(raw) { today }
                    store.initialize(99999)
                    check(profile()==claimed && store.claimDaily()==DailyStatus.ALREADY_CLAIMED)
                    today--
                    check(store.claimDaily()==DailyStatus.ALREADY_CLAIMED && profile()==claimed)
                    today+=10
                    check(store.claimDaily()==DailyStatus.CLAIMED)
                    check(profile().daily.claims==2L && profile().coins==claimed.coins+100)
                    pass("Daily receipt survives reopen, rejects rollback and advances one gift after missed days")

                    raw.edit { it[stringPreferencesKey("profile_v1")]=ProfileCodec.encode(PlayerProfile(150)) }
                    coroutineScope { listOf(async { store.purchase(Booster.HAMMER,"daily-shop") },async { store.claimDaily() }).awaitAll() }
                    check(profile().coins==75L && profile().stock(Booster.HAMMER)==1 && profile().stock(Booster.HINT)==1)
                    pass("Shop and daily reward serialize atomically without lost coins or inventory")
                } finally { job.cancelAndJoin(); file.delete() }
            }
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\n$report\nGATE PASS: $passed checks\n") })
        } catch (error: Throwable) {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", "\n$report\nGATE FAIL: ${error.stackTraceToString()}\n") })
        }
    }
}
