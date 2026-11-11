package com.lavaflow.puzzle.core

import org.junit.Test
import java.io.File

/** Reproducible input to the offline restoration audit; never modifies original content. */
class LegacyExportTest {
    @Test fun exportOriginalCampaign() {
        val rows = LegacyLevelCatalog.levels.map { authored ->
            val d = authored.definition
            val tokens = d.board.tiles.joinToString(" ") { tile ->
                when(tile.kind) {
                    TileKind.STRAIGHT -> "I${tile.turns % 4}"
                    TileKind.CORNER -> "L${tile.turns % 4}"
                    TileKind.T_JUNCTION -> "T${tile.turns % 4}"
                    TileKind.CROSS -> "X"
                    else -> "R"
                }
            }
            """{"id":${d.id},"title":"${authored.title}","size":${d.board.rows},"start":${authored.route.first()},"end":${authored.route.last()},"tokens":"$tokens","spine":${authored.route},"taps":${authored.taps},"limit":${d.moveLimit},"gold":${d.threeStarMoves},"silver":${d.twoStarMoves}}"""
        }
        File("build/legacy-campaign.json").writeText(rows.joinToString(",\n", "[\n", "\n]"))
    }
}
