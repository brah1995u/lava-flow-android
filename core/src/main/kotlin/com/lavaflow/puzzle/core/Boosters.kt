package com.lavaflow.puzzle.core

data class BoosterEffect(val booster: Booster, val indices: List<Int>) {
    init {
        require(indices.distinct().size == indices.size)
        require(indices.size in when (booster) { Booster.HINT -> 2..3; Booster.SWAP -> 2..2; else -> 1..1 })
    }
}

data class SolutionPlan(val input: Board, val route: List<Int>, val rotations: Map<Int, Int>, val cost: Int) {
    fun validates(level: LevelDefinition, board: Board): Boolean {
        if (input != board || route.isEmpty() || route.distinct().size != route.size || rotations.keys != route.toSet()) return false
        if (route.any { it !in board.tiles.indices || !board.tiles[it].kind.playable } || rotations.values.any { it !in 0..3 }) return false
        if (cost != rotations.values.sum() || rotations.any { (i, turns) -> !board.tiles[i].kind.rotatable && turns != 0 }) return false
        if (route.first() != board.index(level.source.adjacent) || route.last() != board.index(level.goal.adjacent)) return false
        val solved = board.copy(tiles = board.tiles.mapIndexed { i, tile -> tile.copy(turns = tile.turns + (rotations[i] ?: 0)) })
        if (level.source.port.opposite !in solved.tiles[route.first()].ports || level.goal.port.opposite !in solved.tiles[route.last()].ports) return false
        return route.zipWithNext().all { (a, b) ->
            Direction.entries.any { direction -> solved.neighbor(a, direction) == b &&
                direction in solved.tiles[a].ports && direction.opposite in solved.tiles[b].ports }
        } && level.trace(solved).goalConnected
    }
}

/** Finds a real route for the CURRENT tile arrangement, including swaps and removed rocks.
 * A bounded simple-path search assigns compatible rotations; BFS still alone decides actual wins.
 * Call on a worker dispatcher. A cancelled/budget-limited search never manufactures an answer. */
object SolutionPlanner {
    fun solve(level: LevelDefinition, board: Board, nodeBudget: Int = 150_000, cancelled: () -> Boolean = { false }): SolutionPlan? {
        require(board.rows == level.board.rows && board.columns == level.board.columns)
        val start = board.index(level.source.adjacent); val goal = board.index(level.goal.adjacent)
        val costs = Array(board.tiles.size) { i ->
            IntArray(16) { pair ->
                val incoming = Direction.entries[pair / 4]; val outgoing = Direction.entries[pair % 4]
                if (!board.tiles[i].kind.playable || incoming == outgoing) -1 else (0..3).firstOrNull { extra ->
                    val ports = board.tiles[i].copy(turns = board.tiles[i].turns + extra).ports
                    incoming in ports && outgoing in ports
                } ?: -1
            }
        }
        var visitedNodes = 0
        var best: SolutionPlan? = null
        val seen = BooleanArray(board.tiles.size)
        val path = ArrayList<Int>(); val turns = LinkedHashMap<Int, Int>()
        fun search(index: Int, incoming: Direction, cost: Int) {
            if (++visitedNodes > nodeBudget || cancelled() || cost >= (best?.cost ?: Int.MAX_VALUE)) return
            seen[index] = true; path.add(index)
            if (index == goal) {
                val extra = costs[index][incoming.ordinal * 4 + level.goal.port.opposite.ordinal]
                if (extra >= 0 && cost + extra < (best?.cost ?: Int.MAX_VALUE)) {
                    turns[index] = extra
                    best = SolutionPlan(board, path.toList(), turns.toMap(), cost + extra)
                    turns.remove(index)
                }
            } else {
                // Prefer inexpensive edges towards the destination, without dropping alternate paths.
                val exits = Direction.entries.mapNotNull { direction ->
                    val next = board.neighbor(index, direction) ?: return@mapNotNull null
                    val extra = costs[index][incoming.ordinal * 4 + direction.ordinal]
                    if (seen[next] || !board.tiles[next].kind.playable || extra < 0) null else Triple(direction, next, extra)
                }.sortedBy { (_, next, extra) -> extra * 10 + kotlin.math.abs(board.cell(next).row - board.cell(goal).row) + kotlin.math.abs(board.cell(next).column - board.cell(goal).column) }
                for ((direction, next, extra) in exits) {
                    turns[index] = extra
                    search(next, direction.opposite, cost + extra)
                    turns.remove(index)
                }
            }
            path.removeAt(path.lastIndex); seen[index] = false
        }
        search(start, level.source.port.opposite, 0)
        return if (cancelled()) null else best?.takeIf { it.validates(level, board) }
    }
}

data class BoosterRequest(val runId: String, val revision: Int, val booster: Booster, val first: Int = -1, val second: Int = -1)
enum class BoosterStatus { APPLIED, STALE, NOT_READY, EMPTY, INVALID_TARGET, NO_EFFECT, NO_SOLUTION }
data class BoosterDecision(val profile: PlayerProfile, val status: BoosterStatus)

object BoosterRules {
    fun swapped(board: Board, first: Int, second: Int): Board? {
        if (first !in board.tiles.indices || second !in board.tiles.indices ||
            !board.tiles[first].kind.playable || !board.tiles[second].kind.playable ||
            Direction.entries.none { board.neighbor(first, it) == second }) return null
        val tiles = board.tiles.toMutableList()
        val old = tiles[first]; tiles[first] = tiles[second]; tiles[second] = old
        return board.copy(tiles = tiles)
    }

    /** One pure decision commits inventory, board, assistance tracking and feedback together. */
    fun apply(profile: PlayerProfile, request: BoosterRequest, plan: SolutionPlan? = null): BoosterDecision {
        fun reject(status: BoosterStatus) = BoosterDecision(profile, status)
        val round = profile.activeRound ?: return reject(BoosterStatus.NOT_READY)
        if (request.runId != round.runId || request.revision != round.revision) return reject(BoosterStatus.STALE)
        if (round.paused || round.phase != RoundPhase.PLAYING) return reject(BoosterStatus.NOT_READY)
        val booster = request.booster
        if (profile.stock(booster) == 0) return reject(BoosterStatus.EMPTY)
        if ((round.boostersUsed[booster] ?: 0) >= 10000) return reject(BoosterStatus.NO_EFFECT)
        var board = round.board
        val targets: List<Int>
        when (booster) {
            Booster.HAMMER -> {
                if (request.first !in board.tiles.indices || board.tiles[request.first].kind != TileKind.BLOCKED) return reject(BoosterStatus.INVALID_TARGET)
                board = board.copy(tiles = board.tiles.mapIndexed { i, tile -> if (i == request.first) Tile(TileKind.CROSS) else tile })
                targets = listOf(request.first)
            }
            Booster.SWAP -> {
                board = swapped(board, request.first, request.second) ?: return reject(BoosterStatus.INVALID_TARGET)
                if (board.tiles.indices.all { board.tiles[it].kind == round.board.tiles[it].kind && board.tiles[it].ports == round.board.tiles[it].ports }) return reject(BoosterStatus.NO_EFFECT)
                if (plan?.validates(round.definition, board) != true) return reject(BoosterStatus.NO_SOLUTION)
                targets = listOf(request.first, request.second)
            }
            Booster.AUTO_ROTATE, Booster.HINT -> {
                if (plan?.validates(round.definition, board) != true) return reject(BoosterStatus.NO_SOLUTION)
                val useful = plan.route.firstOrNull { plan.rotations.getValue(it) > 0 } ?: return reject(BoosterStatus.NO_EFFECT)
                if (booster == Booster.AUTO_ROTATE) {
                    board = board.copy(tiles = board.tiles.mapIndexed { i, tile -> if (i == useful) tile.copy(turns = tile.turns + plan.rotations.getValue(i)) else tile })
                    targets = listOf(useful)
                } else {
                    if (round.effect?.booster == Booster.HINT) return reject(BoosterStatus.NO_EFFECT)
                    val start = minOf(plan.route.indexOf(useful), (plan.route.size - 3).coerceAtLeast(0))
                    targets = plan.route.drop(start).take(3)
                    if (targets.size < 2) return reject(BoosterStatus.NO_EFFECT)
                }
            }
        }
        val nextRound = round.copy(board = board, revision = round.revision + 1,
            phase = if (round.definition.trace(board).goalConnected) RoundPhase.FLOWING else RoundPhase.PLAYING,
            boostersUsed = round.boostersUsed + (booster to (round.boostersUsed[booster] ?: 0) + 1),
            effect = BoosterEffect(booster, targets))
        val next = profile.copy(activeRound = nextRound,
            inventory = profile.inventory + (booster to profile.stock(booster) - 1),
            hammerUses = if (booster == Booster.HAMMER) Math.addExact(profile.hammerUses, 1) else profile.hammerUses,
            hintUses = if (booster == Booster.HINT) Math.addExact(profile.hintUses, 1) else profile.hintUses)
        return BoosterDecision(ProgressRules.updateAchievements(next), BoosterStatus.APPLIED)
    }
}
