package com.lavaflow.puzzle.core

import org.junit.Assert.*
import org.junit.Test

class ProgressRulesTest {
    @Test fun cleanProfileHasOnlyFirstSlotUnlocked() {
        val profile = PlayerProfile(0)
        assertEquals(1, profile.unlockedLevel)
        assertEquals(30, GameCatalog.LEVEL_SLOTS)
        assertTrue(profile.bestStars.isEmpty())
        assertTrue(Achievement.entries.all { it.progress(profile) == 0 })
    }

    @Test fun exactPricesAndSuccessfulPurchase() {
        assertEquals(listOf(150L, 200L, 100L, 250L), Booster.entries.map { it.price })
        Booster.entries.forEach { booster ->
            val result = ProgressRules.purchase(PlayerProfile(booster.price), booster, booster.name)
            assertEquals(PurchaseStatus.PURCHASED, result.status)
            assertEquals(0L, result.profile.coins)
            assertEquals(1, result.profile.stock(booster))
        }
    }

    @Test fun insufficientAndDuplicatePurchasesCannotSpend() {
        val start = PlayerProfile(150)
        assertEquals(start, ProgressRules.purchase(start, Booster.SWAP, "expensive").profile)
        val paid = ProgressRules.purchase(start, Booster.HAMMER, "one")
        assertEquals(paid.profile, ProgressRules.purchase(paid.profile, Booster.HAMMER, "one").profile)
        assertEquals(PurchaseStatus.INSUFFICIENT_COINS, ProgressRules.purchase(paid.profile, Booster.HINT, "two").status)
    }

    @Test fun overflowDoesNotCharge() {
        val start = PlayerProfile(500, inventory = Booster.entries.associateWith { Int.MAX_VALUE })
        val result = ProgressRules.purchase(start, Booster.HAMMER, "full")
        assertEquals(PurchaseStatus.INVENTORY_FULL, result.status)
        assertEquals(start, result.profile)
    }

    @Test fun settingsRemainIndependent() {
        val start = PlayerProfile(0).withSetting(Setting.MUSIC, false).withSetting(Setting.VIBRATION, false)
        assertEquals(FeedbackSettings(false, true, false), start.settings)
    }

    private fun finish(profile: PlayerProfile, run: String, extraMoves: Int): PlayerProfile {
        var round = PuzzleRound(run)
        repeat(extraMoves) { round = PuzzleRules.rotate(round, 4, round.revision) }
        FirstLevel.solutionTaps.forEach { round = PuzzleRules.rotate(round, it, round.revision) }
        return ProgressRules.finishFlow(profile.copy(activeRound = round), run, round.revision)
    }

    @Test fun completedLevelPreservesRecordsAndUnlocksNextContent() {
        var profile = finish(PlayerProfile(0), "run1", 0)
        assertEquals(2, profile.unlockedLevel)
        assertEquals((1..30).toSet(), GameCatalog.playableLevels)
        assertTrue(Achievement.FIRST_FLOW.name in profile.unlockedAchievements)
        assertTrue(Achievement.PERFECT_FLOW.name in profile.unlockedAchievements)
        assertEquals(profile, ProgressRules.finishFlow(profile, "run1", 6))
        profile = finish(profile, "run2", 4)
        assertEquals(3, profile.bestStars[1])
        assertEquals(1, profile.unassistedLevels.size)
        assertEquals(252L, profile.coins)
        assertEquals(6, profile.bestMoves[1])
        assertEquals(68L, profile.activeRound?.awardedCoins)
    }

    @Test fun arrivalCannotAwardUnconnectedPausedOrStaleRounds() {
        val start = PlayerProfile(0, activeRound = PuzzleRound("run"))
        assertEquals(start, ProgressRules.finishFlow(start, "run", 0))
        val won = finish(start, "valid", 0)
        val flowing = won.copy(coins = 0, activeRound = won.activeRound!!.copy(phase = RoundPhase.FLOWING, paused = true, rewardBreakdown = null))
        assertEquals(flowing, ProgressRules.finishFlow(flowing, "valid", 6))
        assertEquals(flowing, ProgressRules.finishFlow(flowing, "old", 6))
    }

    @Test fun achievementThresholdsUseDistinctLevelsAndBestStars() {
        val profile = PlayerProfile(0, unlockedLevel = 20, bestStars = (1..20).associateWith { 3 }, unassistedLevels = (1..20).toSet())
        assertEquals(60, profile.totalStars)
        listOf(Achievement.FLOW_ENGINEER, Achievement.MASTER_ENGINEER, Achievement.NO_HELP_NEEDED, Achievement.LAVA_MASTER)
            .forEach { assertEquals(it.target, it.progress(profile)) }
    }

    @Test fun everyClearPaysAndMoreRemainingMovesPayMore() {
        var profile = finish(PlayerProfile(0), "one", 4)
        assertEquals(168L, profile.coins)
        assertEquals(10, profile.bestMoves[1])
        profile = finish(profile, "two", 2)
        assertEquals(244L, profile.coins)
        assertEquals(76L, profile.activeRound?.awardedCoins)
        profile = finish(profile, "three", 1)
        assertEquals(324L, profile.coins)
        assertEquals(80L, profile.activeRound?.awardedCoins)
        profile = finish(profile, "same-tier-better-moves", 0)
        assertEquals(408L, profile.coins)
        assertEquals(84L, profile.activeRound?.awardedCoins)
        assertEquals(6, profile.bestMoves[1])
        assertEquals(3, profile.bestStars[1])
        assertEquals(1, profile.unassistedLevels.size)
    }

    @Test fun boosterCountersAreIdempotentAndDoNotCountPurchases() {
        var profile = PlayerProfile(10000, activeRound = PuzzleRound("counts"))
        val rocks = profile.activeRound!!.board.tiles.indices.filter { profile.activeRound!!.board.tiles[it].kind == TileKind.BLOCKED }
        rocks.take(10).forEachIndexed { index, cell ->
            profile = ProgressRules.purchase(profile, Booster.HAMMER, "p$index").profile
            val request = BoosterRequest("counts", profile.activeRound!!.revision, Booster.HAMMER, cell)
            profile = BoosterRules.apply(profile, request).profile
            assertEquals(profile, BoosterRules.apply(profile, request).profile)
        }
        assertEquals(10, profile.hammerUses)
        assertEquals(0, profile.hintUses)
        assertTrue(Achievement.ROCK_BREAKER.name in profile.unlockedAchievements)
        assertFalse(Achievement.PATHFINDER.name in profile.unlockedAchievements)
    }
}
