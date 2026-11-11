package com.lavaflow.puzzle.core

import org.junit.Assert.*
import org.junit.Test

class PuzzleTest {
    @Test fun clockwiseRotationAndSymmetricConnections() {
        var corner = Tile(TileKind.CORNER)
        assertEquals(setOf(Direction.NORTH, Direction.EAST), corner.ports)
        corner = corner.rotate()
        assertEquals(setOf(Direction.EAST, Direction.SOUTH), corner.ports)
        repeat(3) { corner = corner.rotate() }
        assertEquals(Tile(TileKind.CORNER).ports, corner.ports)
        assertEquals(Tile(TileKind.STRAIGHT).ports, Tile(TileKind.STRAIGHT, 2).ports)
        assertEquals(Direction.entries.toSet(), Tile(TileKind.CROSS, 3).ports)
        assertEquals(setOf(Direction.NORTH, Direction.EAST, Direction.SOUTH), Tile(TileKind.T_JUNCTION, 1).ports)
    }

    @Test fun fixedKindsCannotRotate() {
        for (kind in listOf(TileKind.BLOCKED, TileKind.SOURCE, TileKind.GOAL)) {
            val tile = Tile(kind)
            assertEquals(tile, tile.rotate())
        }
        assertEquals(setOf(Direction.SOUTH), Tile(TileKind.SOURCE).ports)
        assertEquals(setOf(Direction.NORTH), Tile(TileKind.GOAL).ports)
    }

    @Test fun initialLavaStopsAtTheFirstBrokenConnection() {
        val graph = FirstLevel.definition.trace(FirstLevel.definition.board)
        assertEquals(setOf(2), graph.reachable)
        assertFalse(graph.goalConnected)
        assertTrue(graph.route.isEmpty())
    }

    @Test fun witnessIsGenuineSixMoveWinAndEveryPrefixIsIncomplete() {
        var round = PuzzleRound("witness")
        FirstLevel.solutionTaps.forEachIndexed { step, tile ->
            round = PuzzleRules.rotate(round, tile, round.revision)
            assertEquals(step + 1, round.moves)
            assertEquals(step == 5, round.connectivity.goalConnected)
        }
        assertEquals(RoundPhase.FLOWING, round.phase)
        assertEquals(FirstLevel.intendedRoute, round.connectivity.route)
        assertEquals(3, round.stars)
        assertEquals(round, PuzzleRules.rotate(round, 4, round.revision))
    }

    @Test fun reachableBranchesAdvanceAndWithdrawWithoutFalseWin() {
        var round = PuzzleRound("partial")
        round = PuzzleRules.rotate(round, 7, 0)
        assertEquals(setOf(2, 7, 6, 1), round.connectivity.reachable)
        round = PuzzleRules.rotate(round, 6, 1)
        assertEquals(setOf(2, 7, 6), round.connectivity.reachable)
        round = PuzzleRules.rotate(round, 11, 2)
        assertEquals(setOf(2, 7, 6, 11, 16), round.connectivity.reachable)
        assertFalse(round.connectivity.goalConnected)
    }

    @Test fun reciprocalPortsAndBoardEdgesPreventFalseConnections() {
        val board = Board(2, 2, listOf(Tile(TileKind.STRAIGHT), Tile(TileKind.CORNER), Tile(TileKind.STRAIGHT, 1), Tile(TileKind.STRAIGHT)))
        val graph = PathTraversal.trace(board, Endpoint(Cell(-1,0),Direction.SOUTH,TileKind.SOURCE), Endpoint(Cell(2,1),Direction.NORTH,TileKind.GOAL))
        assertEquals(setOf(0), graph.reachable)
        assertFalse(graph.goalConnected)
        assertNull(board.neighbor(1, Direction.EAST))
        assertNull(board.neighbor(2, Direction.WEST))
        assertNull(board.neighbor(0, Direction.NORTH))
    }

    @Test fun traversalTerminatesOnCyclesAndIncludesAllReachableBranches() {
        val board = Board(2, 2, List(4) { Tile(TileKind.CROSS) })
        val graph = PathTraversal.trace(board, Endpoint(Cell(-1,0),Direction.SOUTH,TileKind.SOURCE), Endpoint(Cell(2,1),Direction.NORTH,TileKind.GOAL))
        assertEquals(setOf(0,1,2,3), graph.reachable)
        assertTrue(graph.goalConnected)
        assertEquals(listOf(0,1,3), graph.route)
    }

    @Test fun sixBySixUsesDimensionsInsteadOfTwentyFiveCells() {
        val tiles = List(36) { if (it % 6 == 3) Tile(TileKind.STRAIGHT) else Tile(TileKind.BLOCKED) }
        val graph = PathTraversal.trace(Board(6,6,tiles), Endpoint(Cell(-1,3),Direction.SOUTH,TileKind.SOURCE), Endpoint(Cell(6,3),Direction.NORTH,TileKind.GOAL))
        assertEquals(listOf(3,9,15,21,27,33), graph.route)
        assertTrue(graph.goalConnected)
    }

    @Test fun blockedPausedStaleAndOutOfRangeActionsSpendNothing() {
        val round = PuzzleRound("reject")
        assertEquals(round, PuzzleRules.rotate(round, 0, 0))
        assertEquals(round, PuzzleRules.rotate(round, -1, 0))
        assertEquals(round, PuzzleRules.rotate(round, 7, 9))
        assertEquals(round.copy(paused = true), PuzzleRules.rotate(round.copy(paused = true), 7, 0))
    }

    @Test fun lastMoveCanWinBeforeExhaustionAndUnconnectedLastMoveLoses() {
        var success = PuzzleRound("last-win")
        repeat(6) { success = PuzzleRules.rotate(success, 4, success.revision) }
        FirstLevel.solutionTaps.forEach { success = PuzzleRules.rotate(success, it, success.revision) }
        assertEquals(12, success.moves)
        assertEquals(RoundPhase.FLOWING, success.phase)
        assertEquals(1, success.stars)
        var failure = PuzzleRound("loss")
        repeat(12) { failure = PuzzleRules.rotate(failure, 4, failure.revision) }
        assertEquals(RoundPhase.LOST, failure.phase)
        assertEquals(failure, PuzzleRules.rotate(failure, 7, failure.revision))
    }

    @Test fun exactStarThresholds() {
        val level = FirstLevel.definition
        assertEquals(3, level.stars(6)); assertEquals(3, level.stars(7))
        assertEquals(2, level.stars(8)); assertEquals(2, level.stars(9))
        assertEquals(1, level.stars(10)); assertEquals(1, level.stars(12))
        assertTrue(runCatching { level.stars(13) }.isFailure)
    }

    @Test fun independentSimplePathSearchProvesUniqueRouteAndSixMoveOptimum() {
        // Enumerate possible simple routes and independently cost the orientations needed at each cell.
        // This is test-only proof over fixed handcrafted content, not procedural level generation.
        val board = FirstLevel.definition.board
        val solutions = mutableListOf<Pair<List<Int>, Int>>()
        fun search(index: Int, incoming: Direction, visited: List<Int>, cost: Int) {
            val tile = board.tiles[index]
            fun rotationCost(exit: Direction): Int? = (0..3).firstOrNull { extra ->
                val ports = tile.kind.basePorts.map { Direction.entries[(it.ordinal + tile.turns + extra) % 4] }
                incoming in ports && exit in ports && incoming != exit
            }
            if (index == 22) rotationCost(Direction.SOUTH)?.let { solutions += (visited + index) to (cost + it) }
            for (exit in Direction.entries) {
                val cell = board.cell(index)
                val r = cell.row + exit.dr; val c = cell.column + exit.dc
                if (r !in 0 until board.rows || c !in 0 until board.columns) continue
                val next = r * board.columns + c
                if (next in visited || board.tiles[next].kind == TileKind.BLOCKED) continue
                val turns = rotationCost(exit) ?: continue
                search(next, exit.opposite, visited + index, cost + turns)
            }
        }
        search(2, Direction.NORTH, emptyList(), 0)
        assertEquals(1, solutions.size)
        assertEquals(FirstLevel.intendedRoute, solutions.single().first)
        assertEquals(6, solutions.single().second)
    }
}
