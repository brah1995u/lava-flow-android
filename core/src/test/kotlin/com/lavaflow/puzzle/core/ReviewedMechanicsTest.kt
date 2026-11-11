package com.lavaflow.puzzle.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ReviewedMechanicsTest {
    private fun reviewedRound(id: String, level: Int) = PuzzleRound(id, board = LevelCatalog.get(level, 2).board, levelId = level, contentVersion = 2)

    private fun play(round: PuzzleRound, taps: List<Int>): PuzzleRound =
        taps.fold(round) { current,index -> PuzzleRules.rotate(current,index,current.revision) }

    @Test fun crossHasNoRotatedStateAndNeverSpendsAMove() {
        val round = reviewedRound("cross",7)
        val index = round.board.tiles.indexOfFirst { it.kind == TileKind.CROSS }
        assertTrue(index >= 0)
        assertEquals(round.board.tiles[index],round.board.tiles[index].rotate())
        assertEquals(round,PuzzleRules.rotate(round,index,round.revision))
        assertEquals(round.board,round.board.rotate(index))
    }

    @Test fun levelFourAcceptsDifferentActionOrdersAndSettlesOnlyOnce() {
        val taps = ReviewedCampaign.levels[3].taps
        assertEquals(7,taps.size)
        for (seed in 0..40) {
            val round = play(reviewedRound("level4-$seed",4),taps.shuffled(Random(seed)))
            assertEquals(RoundPhase.FLOWING,round.phase)
            assertEquals(7,round.moves)
            val profile = PlayerProfile(500,unlockedLevel=4,activeRound=round)
            val won = ProgressRules.finishFlow(profile,round.runId,round.revision)
            assertEquals(RoundPhase.WON,won.activeRound!!.phase)
            assertEquals(746L,won.coins)
            assertEquals(won,ProgressRules.finishFlow(won,round.runId,round.revision))
            assertEquals(won,ProgressRules.finishFlow(won,"old",round.revision))
        }
    }

    @Test fun everyNewWitnessWorksInReverseAndShuffledOrder() {
        for (level in ReviewedCampaign.levels) {
            for (taps in listOf(level.taps.reversed(),level.taps.shuffled(Random(level.definition.id)))) {
                val round = play(reviewedRound("order-${level.definition.id}",level.definition.id),taps)
                assertEquals("Level ${level.definition.id}",RoundPhase.FLOWING,round.phase)
                assertTrue(round.moves <= round.definition.threeStarMoves)
            }
        }
    }

    @Test fun aValidAlternateRouteWinsWithUnusedChannelsStillDry() {
        var round = reviewedRound("alternate",5)
        val alternate = listOf(2,7,6,11,16,17,22)
        alternate.forEachIndexed { position,index ->
            val from = if (position == 0) Direction.NORTH else Direction.entries.first { round.board.neighbor(index,it) == alternate[position-1] }
            val to = if (position == alternate.lastIndex) Direction.SOUTH else Direction.entries.first { round.board.neighbor(index,it) == alternate[position+1] }
            val extra = (0..3).first { amount ->
                val ports = round.board.tiles[index].copy(turns=round.board.tiles[index].turns+amount).ports
                from in ports && to in ports
            }
            repeat(extra) { round=PuzzleRules.rotate(round,index,round.revision) }
        }
        assertEquals(RoundPhase.FLOWING,round.phase)
        assertEquals(alternate,round.connectivity.route)
        assertFalse(12 in round.connectivity.reachable)
        assertTrue(round.board.tiles[12].kind.playable)
        assertNotEquals(ReviewedCampaign.levels[4].route,round.connectivity.route)
    }

    @Test fun boostersCanRescueAChosenRouteThatNoLongerFitsTheRemainingMoves() {
        var round = reviewedRound("rescue",4)
        repeat(9) { round=PuzzleRules.rotate(round,2,round.revision) }
        assertTrue(SolutionPlanner.solve(round.definition,round.board)!!.cost > round.movesRemaining)
        var profile = PlayerProfile(0,unlockedLevel=4,inventory=Booster.entries.associateWith { 3 },activeRound=round)
        repeat(2) {
            val current=profile.activeRound!!
            val result=BoosterRules.apply(profile,BoosterRequest(current.runId,current.revision,Booster.AUTO_ROTATE),
                SolutionPlanner.solve(current.definition,current.board))
            assertEquals(BoosterStatus.APPLIED,result.status)
            profile=result.profile
        }
        val current=profile.activeRound!!
        val plan=SolutionPlanner.solve(current.definition,current.board)!!
        assertEquals(current.movesRemaining,plan.cost)
        round=play(current,plan.route.flatMap { index -> List(plan.rotations.getValue(index)) { index } })
        assertEquals(RoundPhase.FLOWING,round.phase)
        assertEquals(round.definition.moveLimit,round.moves)
        profile=ProgressRules.finishFlow(profile.copy(activeRound=round),round.runId,round.revision)
        assertEquals(1,profile.stock(Booster.AUTO_ROTATE))
        assertEquals(214L,profile.coins)
        assertFalse(4 in profile.unassistedLevels)
    }

    private fun branchLevel(): LevelDefinition {
        val tiles=MutableList(9) { Tile(TileKind.BLOCKED) }
        tiles[1]=Tile(TileKind.T_JUNCTION,1); tiles[2]=Tile(TileKind.CORNER,2)
        tiles[4]=Tile(TileKind.STRAIGHT); tiles[5]=Tile(TileKind.STRAIGHT)
        tiles[7]=Tile(TileKind.T_JUNCTION,1); tiles[8]=Tile(TileKind.CORNER,3)
        return LevelDefinition(99,2,Board(3,3,tiles),Endpoint(Cell(-1,1),Direction.SOUTH,TileKind.SOURCE),
            Endpoint(Cell(3,1),Direction.NORTH,TileKind.GOAL),20,10,15)
    }

    @Test fun splitAndMergePortsFollowRealIncomingNeighbors() {
        val level=branchLevel(); val graph=level.trace(level.board)
        val split=FlowGuides.tile(level,level.board,graph,1)
        assertEquals(setOf(Direction.NORTH),split.inlets)
        assertEquals(setOf(Direction.NORTH,Direction.EAST,Direction.SOUTH),split.joined)
        assertTrue(split.stopped.isEmpty())
        val merge=FlowGuides.tile(level,level.board,graph,8)
        assertEquals(setOf(Direction.NORTH,Direction.WEST),merge.inlets)
        assertTrue(graph.goalConnected)
    }

    @Test fun aStoppedSideBranchDoesNotInvalidateThePortalRoute() {
        val level=branchLevel()
        val board=level.board.copy(tiles=level.board.tiles.mapIndexed { index,tile -> if(index==5) Tile(TileKind.BLOCKED) else tile })
        val graph=level.trace(board)
        assertTrue(graph.goalConnected)
        val stopped=FlowGuides.tile(level,board,graph,2)
        assertEquals(setOf(Direction.WEST),stopped.inlets)
        assertEquals(setOf(Direction.SOUTH),stopped.stopped)
        assertFalse(Direction.SOUTH in stopped.joined)
        assertEquals(FlowGuide(emptySet(),emptySet(),emptySet()),FlowGuides.tile(level,board,graph,0))
    }

    @Test fun forgedCostOrFixedCrossRotationCannotConsumeAnAutoRotate() {
        val round=reviewedRound("plan",7)
        val plan=SolutionPlanner.solve(round.definition,round.board)!!
        assertFalse(plan.copy(cost=-1).validates(round.definition,round.board))
        val cross=plan.route.first { round.board.tiles[it].kind==TileKind.CROSS }
        val forged=plan.copy(rotations=plan.rotations+(cross to 1),cost=plan.cost+1)
        assertFalse(forged.validates(round.definition,round.board))
        val profile=PlayerProfile(0,unlockedLevel=7,inventory=Booster.entries.associateWith { 1 },activeRound=round)
        assertEquals(profile,BoosterRules.apply(profile,BoosterRequest(round.runId,0,Booster.AUTO_ROTATE),forged).profile)
    }

    @Test fun swappingEquivalentStraightOrientationsIsFree() {
        var round=reviewedRound("same",20)
        repeat(3) { round=PuzzleRules.rotate(round,32,round.revision) }
        assertNotEquals(round.board.tiles[32],round.board.tiles[33])
        assertEquals(round.board.tiles[32].ports,round.board.tiles[33].ports)
        val profile=PlayerProfile(0,unlockedLevel=20,inventory=Booster.entries.associateWith { 1 },activeRound=round)
        val result=BoosterRules.apply(profile,BoosterRequest(round.runId,round.revision,Booster.SWAP,32,33))
        assertEquals(BoosterStatus.NO_EFFECT,result.status)
        assertEquals(profile,result.profile)
    }

    @Test fun allLegacyAttemptsKeepTheirOriginalBoardsLimitsAndSolutions() {
        for (level in LegacyLevelCatalog.levels) {
            val initial=PuzzleRound("legacy-${level.definition.id}",board=level.definition.board,levelId=level.definition.id,contentVersion=1)
            assertEquals(level.definition,initial.definition)
            assertEquals(RoundPhase.FLOWING,play(initial,level.taps).phase)
        }
    }

    @Test fun changedPuzzlesKeepSeparateMoveRecordsAndPayReplayReward() {
        val level=ReviewedCampaign.levels[14]
        val before=PlayerProfile(900,unlockedLevel=30,bestStars=mapOf(15 to 3),bestMoves=mapOf(15 to 1),moveRecords=mapOf("1:15" to 1))
        val round=play(reviewedRound("edition2",15),level.taps)
        val won=ProgressRules.finishFlow(before.copy(activeRound=round),round.runId,round.revision)
        assertTrue(won.coins > 900L)
        assertEquals(0L, won.activeRound!!.rewardBreakdown!!.firstClearCoins)
        assertEquals(1,won.bestMovesFor(15,1))
        assertEquals(round.moves,won.bestMovesFor(15,2))
        assertEquals(1,won.bestMoves[15])
    }
}
