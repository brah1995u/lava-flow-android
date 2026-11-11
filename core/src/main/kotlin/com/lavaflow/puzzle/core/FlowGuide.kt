package com.lavaflow.puzzle.core

/** Port-level presentation data derived only from reciprocal connectivity, never from artwork. */
data class FlowGuide(val inlets: Set<Direction>, val joined: Set<Direction>, val stopped: Set<Direction>)

object FlowGuides {
    fun tile(level: LevelDefinition, board: Board, graph: Connectivity, index: Int): FlowGuide {
        if (index !in graph.reachable) return FlowGuide(emptySet(),emptySet(),emptySet())
        val inlets = mutableSetOf<Direction>()
        val joined = mutableSetOf<Direction>()
        val stopped = mutableSetOf<Direction>()
        val source = board.index(level.source.adjacent)
        val goal = board.index(level.goal.adjacent)
        for (port in board.tiles[index].ports) {
            when {
                index == source && port == level.source.port.opposite -> { inlets += port; joined += port }
                index == goal && port == level.goal.port.opposite -> joined += port
                else -> {
                    val neighbor = board.neighbor(index,port)
                    if (neighbor != null && neighbor in graph.reachable && port.opposite in board.tiles[neighbor].ports) {
                        joined += port
                        if (graph.depths.getValue(neighbor) < graph.depths.getValue(index)) inlets += port
                    } else stopped += port
                }
            }
        }
        return FlowGuide(inlets,joined,stopped)
    }
}
