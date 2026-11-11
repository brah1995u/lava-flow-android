package com.lavaflow.puzzle.core

// Edition 1 is retained exclusively for compatible saved attempts.
object LegacyFirstLevel {
    // Original Level 1 remains byte-for-byte equivalent content. Coordinates are zero-based, row-major.
    private fun t(kind: TileKind, turns: Int = 0) = Tile(kind, turns)
    private val rock = t(TileKind.BLOCKED)
    val definition = LevelDefinition(1, 1, Board(5, 5, listOf(
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

object LegacyLevelCatalog {
    private fun authored(id: Int, title: String, layout: String, limit: Int, gold: Int, silver: Int,
                         route: List<Int>, taps: List<Int>): AuthoredLevel {
        val tiles = layout.trim().split(Regex("\\s+")).map { token ->
            Tile(when (token[0]) {
                'I' -> TileKind.STRAIGHT; 'L' -> TileKind.CORNER; 'T' -> TileKind.T_JUNCTION
                'X' -> TileKind.CROSS; else -> TileKind.BLOCKED
            }, token.drop(1).toIntOrNull() ?: 0)
        }
        return AuthoredLevel(title, LevelDefinition(id, 1, Board(5, 5, tiles),
            Endpoint(Cell(-1, 2), Direction.SOUTH, TileKind.SOURCE),
            Endpoint(Cell(5, 2), Direction.NORTH, TileKind.GOAL), limit, gold, silver), route, taps)
    }
    val levels = listOf(
        AuthoredLevel("The First Ember", LegacyFirstLevel.definition, LegacyFirstLevel.intendedRoute, LegacyFirstLevel.solutionTaps),
        authored(2, "Fork in the Fire", """
            R R I0 T2 R
            R I1 L3 L1 I0
            L0 L0 X L2 R
            R I1 R T0 L1
            I1 L3 L1 R R
        """, 13, 8, 10, listOf(2,7,8,13,12,11,16,21,22), listOf(7,8,13,11,16,21,22)),
        authored(3, "Basalt Bend", """
            I1 L0 L2 R L3
            L0 T2 R I1 R
            I1 R L0 T1 L3
            L3 X L3 I0 R
            R R L1 L2 I1
        """, 20, 13, 16, listOf(2,1,6,5,10,15,16,17,12,13,18,23,22), listOf(2,1,6,5,10,15,12,13,23)),
        authored(4, "The Crucible", """
            R R L3 L0 I1
            L0 T0 L0 L3 L1
            I1 I1 X I0 L2
            R L0 I1 L1 R
            I1 R L1 L3 T1
        """, 23, 16, 19, listOf(2,3,8,9,14,13,12,7,6,11,16,17,18,23,22), listOf(2,3,3,8,9,14,13,7,7,6,11,18)),
        authored(5, "Heart of the Volcano", """
            L0 I0 L1 L0 L1
            L3 I0 X L1 I1
            I1 R R L0 L1
            L2 L1 T1 L3 I1
            I1 L0 L2 R R
        """, 27, 19, 23, listOf(2,1,0,5,6,7,8,3,4,9,14,13,18,17,16,21,22), listOf(2,2,1,0,5,6,8,8,3,4,9,14,14,13,17))
    ).let { firstFive -> firstFive + (6..30).map { id -> buildDeterministic(id, firstFive) } }

    private fun buildDeterministic(id: Int, firstFive: List<AuthoredLevel>): AuthoredLevel {
        val size = if (id < 13) 5 else 6
        val routeA = listOf(2,8,7,6,12,18,19,20,14,15,16,22,28,27,33)
        val routeB = listOf(2,1,7,8,9,3,4,10,16,17,23,29,28,22,21,20,19,25,31,32,33)
        val routeC = listOf(2,1,0,6,7,8,9,3,4,5,11,17,16,15,14,20,19,18,24,30,31,25,26,27,33)
        val routeD = listOf(2,1,0,6,12,18,24,30,31,32,26,20,14,15,16,10,4,5,11,17,23,29,35,34,33)
        val routeE = listOf(2,3,4,5,11,17,23,29,35,34,28,22,16,15,14,20,26,32,31,25,19,13,7,6,12,18,24,30)
        val base = when {
            id <= 8 -> firstFive[2].route
            id <= 12 -> firstFive[if (id % 2 == 0) 3 else 4].route
            id <= 15 -> routeA
            id <= 19 -> routeB
            id <= 22 -> routeC
            id % 3 == 0 -> routeE
            else -> routeD
        }
        val path = base.map { if (id % 2 == 0) it / size * size + size - 1 - it % size else it }
        val random = java.util.Random(0x1A7AF10L + id * 7919L)
        val tiles = MutableList(size * size) { Tile(TileKind.BLOCKED) }
        fun direction(a: Int, b: Int) = Direction.entries.first { a / size + it.dr == b / size && a % size + it.dc == b % size }
        val ports = path.mapIndexed { i, cell ->
            (if (i == 0) Direction.NORTH else direction(cell, path[i - 1])) to
                (if (i == path.lastIndex) Direction.SOUTH else direction(cell, path[i + 1]))
        }
        path.forEachIndexed { position, cell ->
            val (incoming, outgoing) = ports[position]
            val baseKind = if (incoming.opposite == outgoing) TileKind.STRAIGHT else TileKind.CORNER
            val extraPathNeighbor = Direction.entries.any { direction ->
                val r = cell / size + direction.dr; val c = cell % size + direction.dc
                r in 0 until size && c in 0 until size && r * size + c in path &&
                    r * size + c != path.getOrNull(position - 1) && r * size + c != path.getOrNull(position + 1)
            }
            val kind = when {
                position == 0 || position == path.lastIndex || extraPathNeighbor -> baseKind
                position % (if (id >= 23) 8 else 11) == 4 -> TileKind.CROSS
                position % (if (id >= 16) 4 else 6) == 2 -> TileKind.T_JUNCTION
                else -> baseKind
            }
            val solved = (0..3).first { turn -> Tile(kind, turn).ports.let { incoming in it && outgoing in it } }
            tiles[cell] = Tile(kind, (solved + random.nextInt(4)) % 4)
        }
        // Decoys are deterministic authored-template decoration; they never remove the witness route.
        val decoys = mutableSetOf<Int>()
        for (cell in tiles.indices.filter { it !in path }.shuffled(kotlin.random.Random(id))) {
            val neighbors = Direction.entries.mapNotNull { direction ->
                val r = cell / size + direction.dr; val c = cell % size + direction.dc
                if (r in 0 until size && c in 0 until size) r * size + c else null
            }
            if (neighbors.count { it in path } > 1 || neighbors.any { it in decoys } || decoys.size >= 4) continue
            decoys += cell
            tiles[cell] = Tile(if (random.nextBoolean()) TileKind.CORNER else TileKind.STRAIGHT, random.nextInt(4))
        }
        // A dry source port makes an accidentally pre-solved start impossible.
        while (Direction.NORTH in tiles[path.first()].ports) tiles[path.first()] = tiles[path.first()].rotate()
        val taps = path.flatMapIndexed { i, cell ->
            val (incoming, outgoing) = ports[i]
            val extra = (0..3).first { turn -> tiles[cell].copy(turns = tiles[cell].turns + turn).ports.let { incoming in it && outgoing in it } }
            List(extra) { cell }
        }
        val gold = taps.size + if (id <= 8) 5 else if (id <= 15) 4 else 2
        val silver = gold + if (id <= 15) 5 else 4
        val titles = listOf("Cinder Crossing", "Ember Arches", "Molten Steps", "Split Current", "Ashen Junction",
            "Obsidian Trail", "Golden Veins", "The Wider Rift", "Crater Road", "Forked Basin", "Deep Channels",
            "Fire Labyrinth", "Ancient Circuit", "Ember Spires", "Under the Caldera", "Molten Maze", "Basalt Crown",
            "Twisting Depths", "The Long Descent", "Furnace Web", "Obsidian Heart", "Crimson Passage", "Portal Forge",
            "Final Ascent", "Master of the Flow")
        return AuthoredLevel(titles[id - 6], LevelDefinition(id, 1, Board(size, size, tiles.toList()),
            Endpoint(Cell(-1, path.first() % size), Direction.SOUTH, TileKind.SOURCE),
            Endpoint(Cell(size, path.last() % size), Direction.NORTH, TileKind.GOAL), silver + 5, gold, silver), path, taps)
    }
    fun get(id: Int): LevelDefinition = levels.firstOrNull { it.definition.id == id }?.definition
        ?: error("Unsupported level $id")
    fun title(id: Int) = levels.first { it.definition.id == id }.title
}

