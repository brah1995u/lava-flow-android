package com.lavaflow.puzzle.core

enum class Direction(val dr: Int, val dc: Int) {
    NORTH(-1, 0), EAST(0, 1), SOUTH(1, 0), WEST(0, -1);
    val opposite get() = entries[(ordinal + 2) % 4]
    fun clockwise(turns: Int) = entries[(ordinal + turns % 4) % 4]
}

enum class TileKind(val basePorts: Set<Direction>, val playable: Boolean = true) {
    STRAIGHT(setOf(Direction.NORTH, Direction.SOUTH)),
    CORNER(setOf(Direction.NORTH, Direction.EAST)),
    T_JUNCTION(setOf(Direction.NORTH, Direction.EAST, Direction.WEST)),
    CROSS(Direction.entries.toSet()),
    BLOCKED(emptySet(), false),
    SOURCE(setOf(Direction.SOUTH), false),
    GOAL(setOf(Direction.NORTH), false);

    val rotatable get() = playable && this != CROSS
}

data class Tile(val kind: TileKind, val turns: Int = 0) {
    init { require(turns >= 0) }
    val ports get() = kind.basePorts.map { it.clockwise(turns) }.toSet()
    fun rotate() = if (kind.rotatable) copy(turns = Math.addExact(turns, 1)) else this
}

data class Cell(val row: Int, val column: Int)
data class Board(val rows: Int, val columns: Int, val tiles: List<Tile>) {
    init { require(rows in 1..20 && columns in 1..20 && tiles.size == rows * columns) }
    fun contains(cell: Cell) = cell.row in 0 until rows && cell.column in 0 until columns
    fun index(cell: Cell): Int { require(contains(cell)); return cell.row * columns + cell.column }
    fun cell(index: Int): Cell { require(index in tiles.indices); return Cell(index / columns, index % columns) }
    fun neighbor(index: Int, direction: Direction): Int? {
        val cell = cell(index)
        val next = Cell(cell.row + direction.dr, cell.column + direction.dc)
        return if (contains(next)) this.index(next) else null
    }
    fun rotate(index: Int) = copy(tiles = tiles.mapIndexed { i, tile -> if (i == index) tile.rotate() else tile })
}

/** A fixed source/goal outside the grid, facing an edge cell. */
data class Endpoint(val cell: Cell, val port: Direction, val kind: TileKind) {
    init { require(kind == TileKind.SOURCE || kind == TileKind.GOAL) }
    val adjacent get() = Cell(cell.row + port.dr, cell.column + port.dc)
}

data class Connectivity(val depths: Map<Int, Int>, val parents: Map<Int, Int>, val goalConnected: Boolean, val route: List<Int>) {
    val reachable get() = depths.keys
}

object PathTraversal {
    fun trace(board: Board, source: Endpoint, goal: Endpoint): Connectivity {
        require(!board.contains(source.cell) && !board.contains(goal.cell))
        require(board.contains(source.adjacent) && board.contains(goal.adjacent))
        val entry = board.index(source.adjacent)
        val exit = board.index(goal.adjacent)
        if (source.port.opposite !in board.tiles[entry].ports) return Connectivity(emptyMap(), emptyMap(), false, emptyList())
        val depths = linkedMapOf(entry to 0)
        val parents = mutableMapOf<Int, Int>()
        val queue = ArrayDeque<Int>()
        queue.add(entry)
        while (queue.isNotEmpty()) {
            val here = queue.removeFirst()
            for (direction in Direction.entries) {
                if (direction !in board.tiles[here].ports) continue
                val next = board.neighbor(here, direction) ?: continue
                if (direction.opposite !in board.tiles[next].ports || next in depths) continue
                depths[next] = depths.getValue(here) + 1
                parents[next] = here
                queue.add(next)
            }
        }
        val connected = exit in depths && goal.port.opposite in board.tiles[exit].ports
        val route = if (connected) buildList {
            var cursor = exit
            add(cursor)
            while (cursor != entry) { cursor = parents.getValue(cursor); add(cursor) }
        }.reversed() else emptyList()
        return Connectivity(depths, parents, connected, route)
    }
}

data class LevelDefinition(
    val id: Int, val contentVersion: Int, val board: Board,
    val source: Endpoint, val goal: Endpoint,
    val moveLimit: Int, val threeStarMoves: Int, val twoStarMoves: Int
) {
    init { require(threeStarMoves in 1..twoStarMoves && twoStarMoves <= moveLimit) }
    fun trace(board: Board) = PathTraversal.trace(board, source, goal)
    fun stars(moves: Int): Int {
        require(moves in 0..moveLimit)
        return when { moves <= threeStarMoves -> 3; moves <= twoStarMoves -> 2; else -> 1 }
    }
}

object FirstLevel {
    // Reviewed tutorial retains its known layout; edition 1 remains in LegacyLevels for old saves.
    private fun t(kind: TileKind, turns: Int = 0) = Tile(kind, turns)
    private val rock = t(TileKind.BLOCKED)
    val definition = LevelDefinition(1, 3, Board(5, 5, listOf(
        rock, t(TileKind.CORNER, 1), t(TileKind.STRAIGHT), rock, t(TileKind.STRAIGHT, 1),
        rock, t(TileKind.CORNER), t(TileKind.CORNER, 2), rock, t(TileKind.CORNER, 3),
        t(TileKind.CORNER, 1), t(TileKind.STRAIGHT, 3), rock, t(TileKind.STRAIGHT), rock,
        rock, t(TileKind.CORNER, 3), t(TileKind.STRAIGHT, 1), t(TileKind.CORNER, 1), rock,
        t(TileKind.STRAIGHT, 1), rock, t(TileKind.CORNER), t(TileKind.CORNER, 3), rock
    )), Endpoint(Cell(-1, 2), Direction.SOUTH, TileKind.SOURCE),
        Endpoint(Cell(5, 2), Direction.NORTH, TileKind.GOAL), 12, 7, 9)

    // A proof witness, never used to force a win or guide game rules.
    val solutionTaps = listOf(7, 6, 11, 16, 18, 22)
    val intendedRoute = listOf(2, 7, 6, 11, 16, 17, 18, 23, 22)
}

enum class RoundPhase { PLAYING, FLOWING, WON, LOST }
data class PuzzleRound(
    val runId: String, val board: Board = FirstLevel.definition.board,
    val moves: Int = 0, val revision: Int = 0, val phase: RoundPhase = RoundPhase.PLAYING,
    val paused: Boolean = false, val awardedCoins: Long = 0,
    val levelId: Int = 1,
    val boostersUsed: Map<Booster, Int> = emptyMap(),
    val effect: BoosterEffect? = null,
    val contentVersion: Int = LevelCatalog.CONTENT_VERSION,
    val rewardBreakdown: CompletionReward? = null
) {
    init {
        val level = LevelCatalog.get(levelId, contentVersion)
        require(runId.isNotBlank() && moves in 0..level.moveLimit && revision >= 0 && awardedCoins >= 0)
        rewardBreakdown?.let { require(phase == RoundPhase.WON && it.total == awardedCoins) }
        require(board.rows == level.board.rows && board.columns == level.board.columns)
        require(boostersUsed.values.all { it in 1..10000 })
        require(revision.toLong() == moves.toLong() + boostersUsed.values.sumOf { it.toLong() })
        val initial = level.board.tiles
        if (boostersUsed.isEmpty()) {
            require(board.tiles.map { it.kind } == initial.map { it.kind })
            require(board.tiles.indices.all { board.tiles[it].turns in initial[it].turns..initial[it].turns + level.moveLimit })
            require(board.tiles.indices.sumOf { board.tiles[it].turns - initial[it].turns } == moves)
            require(board.tiles.indices.all { board.tiles[it].kind.playable || board.tiles[it] == initial[it] })
        } else {
            // Swaps preserve material counts; each Hammer replaces exactly one rock with a cross.
            val removed = boostersUsed[Booster.HAMMER] ?: 0
            TileKind.entries.forEach { kind ->
                val expected = initial.count { it.kind == kind } + when (kind) {
                    TileKind.CROSS -> removed; TileKind.BLOCKED -> -removed; else -> 0
                }
                require(board.tiles.count { it.kind == kind } == expected)
            }
            require(board.tiles.all { it.turns <= 3L + revision.toLong() * 3 })
            require(board.tiles.indices.all { board.tiles[it].kind != TileKind.BLOCKED || initial[it].kind == TileKind.BLOCKED })
        }
        effect?.let { require((boostersUsed[it.booster] ?: 0) > 0 && it.indices.all { index -> index in board.tiles.indices }) }
        val connected = level.trace(board).goalConnected
        require(when (phase) {
            RoundPhase.PLAYING -> moves < level.moveLimit && !connected
            RoundPhase.FLOWING, RoundPhase.WON -> revision > 0 && connected
            RoundPhase.LOST -> moves == level.moveLimit && !connected
        })
    }
    val definition get() = LevelCatalog.get(levelId, contentVersion)
    val connectivity get() = definition.trace(board)
    val movesRemaining get() = definition.moveLimit - moves
    val stars get() = if (phase == RoundPhase.WON || phase == RoundPhase.FLOWING) definition.stars(moves) else 0
    companion object {
        fun start(runId: String, level: Int) = PuzzleRound(runId, board = LevelCatalog.get(level).board, levelId = level)
    }
}

object PuzzleRules {
    fun rotate(round: PuzzleRound, index: Int, expectedRevision: Int): PuzzleRound {
        if (round.paused || round.phase != RoundPhase.PLAYING || round.revision != expectedRevision ||
            index !in round.board.tiles.indices || !round.board.tiles[index].kind.rotatable) return round
        val board = round.board.rotate(index)
        val moves = round.moves + 1
        val phase = when {
            round.definition.trace(board).goalConnected -> RoundPhase.FLOWING
            moves == round.definition.moveLimit -> RoundPhase.LOST
            else -> RoundPhase.PLAYING
        }
        return round.copy(board = board, moves = moves, revision = round.revision + 1, phase = phase, effect = null)
    }
    fun reward(stars: Int): Long = when (stars) { 0 -> 0; 1 -> 150; 2 -> 200; 3 -> 250; else -> error("Invalid stars") }
}
