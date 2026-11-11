package com.lavaflow.puzzle.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class RestoredCampaignTest {
    @Test fun everyLaterBoardAcceptsAtLeastTwoDifferentActualWinningPaths() {
        for((id,witnesses) in alternativeWitnesses) {
            val paths=witnesses.mapIndexed { index,taps ->
                var round=PuzzleRound.start("alternative-$id-$index",id)
                for(tile in taps) round=PuzzleRules.rotate(round,tile,round.revision)
                assertEquals("$id, $index",RoundPhase.FLOWING,round.phase)
                assertTrue(round.moves<=round.definition.moveLimit)
                round.connectivity.route
            }.toSet()
            if(id>=4) assertTrue("Level $id has only ${paths.size} real winning paths",paths.size>=2)
        }
    }
    @Test fun toolsKeepAValidatedRouteOnEveryRestoredBoard() {
        for(level in LevelCatalog.levels) {
            var profile=PlayerProfile(0,unlockedLevel=level.definition.id,
                inventory=Booster.entries.associateWith { 3 },activeRound=PuzzleRound.start("tools-${level.definition.id}",level.definition.id))
            fun use(booster: Booster, first: Int = -1, second: Int = -1) {
                val round=profile.activeRound!!
                val board=if(booster==Booster.SWAP) BoosterRules.swapped(round.board,first,second)!! else round.board
                val decision=BoosterRules.apply(profile,BoosterRequest(round.runId,round.revision,booster,first,second),SolutionPlanner.solve(round.definition,board))
                assertEquals("${level.definition.id}: $booster",BoosterStatus.APPLIED,decision.status)
                profile=decision.profile
                assertEquals(2,profile.stock(booster));assertEquals(0,profile.activeRound!!.moves)
            }
            use(Booster.HAMMER,profile.activeRound!!.board.tiles.indexOfFirst { it.kind==TileKind.BLOCKED })
            use(Booster.AUTO_ROTATE)
            use(Booster.HINT)
            val round=profile.activeRound!!
            val pair=round.board.tiles.indices.flatMap { a -> Direction.entries.mapNotNull { d -> round.board.neighbor(a,d)?.let { a to it } } }
                .first { (a,b) ->
                    val candidate=BoosterRules.swapped(round.board,a,b)
                    candidate!=null && candidate.tiles.map { it.ports }!=round.board.tiles.map { it.ports } &&
                        SolutionPlanner.solve(round.definition,candidate)!=null
                }
            use(Booster.SWAP,pair.first,pair.second)
            val final=profile.activeRound!!
            assertNotNull(SolutionPlanner.solve(final.definition,final.board))
        }
    }
    @Test fun originalBoardsRemainRecognizableAndTheirRoutesRemainPossible() {
        for(level in LevelCatalog.levels) {
            val old=LegacyLevelCatalog.levels[level.definition.id-1]
            val board=level.definition.board
            assertEquals(old.definition.board.rows,board.rows)
            assertEquals(old.definition.source,level.definition.source)
            assertEquals(old.definition.goal,level.definition.goal)
            assertTrue(board.tiles.indices.count { board.tiles[it].kind!=old.definition.board.tiles[it].kind } <= 2)
            assertTrue(board.tiles.indices.all { !old.definition.board.tiles[it].kind.playable || board.tiles[it].kind.playable })
            old.route.forEachIndexed { position,index ->
                val incoming=if(position==0) Direction.NORTH else Direction.entries.first { board.neighbor(index,it)==old.route[position-1] }
                val outgoing=if(position==old.route.lastIndex) Direction.SOUTH else Direction.entries.first { board.neighbor(index,it)==old.route[position+1] }
                assertTrue((0..3).any { turn -> Tile(board.tiles[index].kind,turn).ports.containsAll(setOf(incoming,outgoing)) })
            }
        }
    }
    @Test fun minimumEffortGrowsAndWitnessesWorkInMultipleActionOrders() {
        var previous=0
        for(level in LevelCatalog.levels) {
            val plan=SolutionPlanner.solve(level.definition,level.definition.board)!!
            assertTrue(plan.cost>=previous);previous=plan.cost
            for(seed in 0..6) {
                var round=PuzzleRound.start("restored-${level.definition.id}-$seed",level.definition.id)
                for(index in level.taps.shuffled(Random(seed))) round=PuzzleRules.rotate(round,index,round.revision)
                assertEquals(RoundPhase.FLOWING,round.phase)
                assertEquals(3,round.stars)
            }
        }
        assertEquals(28,previous)
    }
}
