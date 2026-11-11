package com.lavaflow.puzzle.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ContentAndBoostersTest {
    private fun stocked(round: PuzzleRound = PuzzleRound("boost")) = PlayerProfile(0,
        inventory = Booster.entries.associateWith { 3 }, activeRound = round)
    private fun request(profile: PlayerProfile, booster: Booster, first: Int = -1, second: Int = -1) =
        BoosterRequest(profile.activeRound!!.runId, profile.activeRound!!.revision, booster, first, second)

    @Test fun allThirtyLevelsHaveValidDistinctStartsAndOrdinaryActionWitnesses() {
        assertEquals((1..30).toList(), LevelCatalog.levels.map { it.definition.id })
        assertEquals(30, LevelCatalog.levels.map { it.definition.board }.distinct().size)
        val reports = LevelCatalog.levels.map { authored ->
            val level = authored.definition
            assertTrue(level.board.rows in 5..6 && level.board.columns in 5..6)
            assertFalse("Level ${level.id} starts solved", level.trace(level.board).goalConnected)
            assertEquals(authored.route.size, authored.route.distinct().size)
            var round = PuzzleRound.start("validation-${level.id}", level.id)
            for (tile in authored.taps) round = PuzzleRules.rotate(round, tile, round.revision)
            assertEquals("Level ${level.id} witness", RoundPhase.FLOWING, round.phase)
            assertTrue(round.connectivity.goalConnected)
            assertTrue(round.moves in 1..level.threeStarMoves)
            assertEquals(3, level.stars(level.threeStarMoves))
            assertEquals(2, level.stars(level.threeStarMoves + 1))
            assertEquals(2, level.stars(level.twoStarMoves))
            assertEquals(1, level.stars(level.twoStarMoves + 1))
            assertEquals(1, level.stars(level.moveLimit))
            // An independent route witness uses real reciprocal ports, including all new tile kinds.
            val plan = SolutionPlanner.solve(level, level.board)
            assertNotNull("Level ${level.id} planner", plan)
            assertTrue(plan!!.validates(level, level.board))
            assertTrue(plan.cost <= authored.taps.size)
            """{"level":${level.id},"rows":${level.board.rows},"title":"${authored.title}","moves":${round.moves},"limit":${level.moveLimit},"gold":${level.threeStarMoves},"silver":${level.twoStarMoves},"plannerCost":${plan.cost},"route":${authored.route},"taps":${authored.taps}}"""
        }
        File("build/level-validation.json").writeText(reports.joinToString(",\n", "[\n", "\n]"))
    }

    @Test fun fullCampaignUnlocksThirtyAndNeverThirtyOneAndPaysEachCompletedAttemptOnce() {
        var profile = PlayerProfile(0)
        var expectedCoins = 0L
        LevelCatalog.levels.forEach { authored ->
            val id = authored.definition.id
            assertEquals(id, profile.unlockedLevel)
            var round = PuzzleRound.start("campaign-$id", id)
            authored.taps.forEach { round = PuzzleRules.rotate(round, it, round.revision) }
            profile = ProgressRules.finishFlow(profile.copy(activeRound = round), round.runId, round.revision)
            assertEquals((id + 1).coerceAtMost(30), profile.unlockedLevel)
            expectedCoins += 160L + (id - 1) * 18L + round.movesRemaining * (4L + (id - 1) / 10)
            assertEquals(expectedCoins, profile.coins)
            assertEquals(profile, ProgressRules.finishFlow(profile, round.runId, round.revision))
        }
        assertEquals(90, profile.totalStars)
        assertEquals(30, profile.unassistedLevels.size)
        assertTrue(setOf("GRAND_FLOW", "PERFECTION", "LAVA_MASTER", "FLOW_ENGINEER", "MASTER_ENGINEER", "NO_HELP_NEEDED").all { it in profile.unlockedAchievements })
    }

    @Test fun hammerChargesOnlyForOneRealRockAndNeverCostsAMove() {
        val before = stocked()
        assertEquals(before, BoosterRules.apply(before, request(before, Booster.HAMMER, 7)).profile)
        assertEquals(before, BoosterRules.apply(before, request(before, Booster.HAMMER, -1)).profile)
        val command = request(before, Booster.HAMMER, 0)
        val result = BoosterRules.apply(before, command)
        assertEquals(BoosterStatus.APPLIED, result.status)
        assertEquals(TileKind.CROSS, result.profile.activeRound!!.board.tiles[0].kind)
        assertEquals(2, result.profile.stock(Booster.HAMMER))
        assertEquals(0, result.profile.activeRound!!.moves)
        assertEquals(1, result.profile.hammerUses)
        assertEquals(result.profile, BoosterRules.apply(result.profile, command).profile)
        assertEquals(result.profile, BoosterRules.apply(result.profile, request(result.profile, Booster.HAMMER, 0)).profile)
    }

    @Test fun autoRotateAffectsOneWrongUsefulTileOfCurrentSolutionAndCannotDoubleCharge() {
        val before = stocked()
        val round = before.activeRound!!
        val plan = SolutionPlanner.solve(round.definition, round.board)!!
        val command = request(before, Booster.AUTO_ROTATE)
        val result = BoosterRules.apply(before, command, plan)
        assertEquals(BoosterStatus.APPLIED, result.status)
        val after = result.profile.activeRound!!
        val changed = round.board.tiles.indices.filter { round.board.tiles[it] != after.board.tiles[it] }
        assertEquals(1, changed.size)
        assertTrue(changed.single() in plan.route && plan.rotations.getValue(changed.single()) > 0)
        assertEquals(0, after.moves)
        assertEquals(2, result.profile.stock(Booster.AUTO_ROTATE))
        assertEquals(result.profile, BoosterRules.apply(result.profile, command, plan).profile)
        assertEquals(BoosterStatus.NO_SOLUTION, BoosterRules.apply(result.profile, request(result.profile, Booster.AUTO_ROTATE), plan).status)
    }

    @Test fun hintsPersistUsefulTilesWithoutRotatingAndRepeatOnSameBoardIsFree() {
        var round = PuzzleRound("near")
        FirstLevel.solutionTaps.dropLast(1).forEach { round = PuzzleRules.rotate(round, it, round.revision) }
        val before = stocked(round)
        val plan = SolutionPlanner.solve(round.definition, round.board)!!
        val result = BoosterRules.apply(before, request(before, Booster.HINT), plan).profile
        val after = result.activeRound!!
        assertEquals(round.board, after.board)
        assertEquals(round.moves, after.moves)
        assertTrue(after.effect!!.indices.size in 2..3 && 22 in after.effect.indices)
        assertTrue(after.effect.indices.all { it in plan.route })
        assertEquals(2, result.stock(Booster.HINT))
        assertEquals(result, BoosterRules.apply(result, request(result, Booster.HINT), plan).profile)
        val played = result.copy(activeRound = PuzzleRules.rotate(after, 22, after.revision))
        val won = ProgressRules.finishFlow(played, "near", played.activeRound!!.revision)
        assertFalse(1 in won.unassistedLevels)
        assertEquals(184L, won.coins)
    }

    @Test fun swapRejectsRocksEdgesNonAdjacentSameTilesAndUnsolvableMutations() {
        val before = stocked()
        for ((first, second) in listOf(0 to 1, -1 to 2, 4 to 5, 1 to 22, 1 to 1)) {
            assertNull(BoosterRules.swapped(before.activeRound!!.board, first, second))
            assertEquals(before, BoosterRules.apply(before, request(before, Booster.SWAP, first, second)).profile)
        }
        val candidate = BoosterRules.swapped(before.activeRound!!.board, 1, 6)!!
        val plan = SolutionPlanner.solve(before.activeRound!!.definition, candidate)!!
        val result = BoosterRules.apply(before, request(before, Booster.SWAP, 1, 6), plan)
        assertEquals(BoosterStatus.APPLIED, result.status)
        assertEquals(candidate, result.profile.activeRound!!.board)
        assertEquals(0, result.profile.activeRound!!.moves)
        assertEquals(2, result.profile.stock(Booster.SWAP))
        assertTrue(SolutionPlanner.solve(result.profile.activeRound!!.definition, candidate)!!.validates(result.profile.activeRound!!.definition, candidate))
        assertEquals(before, BoosterRules.apply(before, request(before, Booster.SWAP, 1, 6), null).profile)
    }

    @Test fun emptyPausedTerminalStaleOrCancelledBoosterSearchesNeverConsume() {
        val before = stocked()
        val empty = before.copy(inventory = Booster.entries.associateWith { 0 })
        Booster.entries.forEach { booster ->
            assertEquals(BoosterStatus.EMPTY, BoosterRules.apply(empty, request(empty, booster)).status)
            val paused = before.copy(activeRound = before.activeRound!!.copy(paused = true))
            assertEquals(paused, BoosterRules.apply(paused, request(paused, booster)).profile)
            assertEquals(before, BoosterRules.apply(before, BoosterRequest("old", 0, booster)).profile)
        }
        assertNull(SolutionPlanner.solve(before.activeRound!!.definition, before.activeRound!!.board, cancelled = { true }))
        var round = before.activeRound!!
        FirstLevel.solutionTaps.forEach { round = PuzzleRules.rotate(round, it, round.revision) }
        val solved = before.copy(activeRound = round)
        assertEquals(solved, BoosterRules.apply(solved, request(solved, Booster.AUTO_ROTATE), SolutionPlanner.solve(round.definition, round.board)).profile)
    }

    @Test fun boostersCanFinishAZeroMoveAssistedRoundAndCannotEarnUnassistedCredit() {
        var profile = stocked().copy(inventory = Booster.entries.associateWith { 20 })
        repeat(10) {
            val round = profile.activeRound!!
            if (round.phase == RoundPhase.PLAYING) profile = BoosterRules.apply(profile, request(profile, Booster.AUTO_ROTATE), SolutionPlanner.solve(round.definition, round.board)).profile
        }
        assertEquals(RoundPhase.FLOWING, profile.activeRound!!.phase)
        assertEquals(0, profile.activeRound!!.moves)
        profile = ProgressRules.finishFlow(profile, "boost", profile.activeRound!!.revision)
        assertEquals(3, profile.bestStars[1]); assertEquals(0, profile.bestMoves[1])
        assertFalse(1 in profile.unassistedLevels)
    }
}
