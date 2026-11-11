package com.lavaflow.puzzle.core

data class AuthoredLevel(val title: String, val definition: LevelDefinition, val route: List<Int>, val taps: List<Int>, val lesson: String = "")

internal fun authored(id: Int, title: String, lesson: String, size: Int, sourceColumn: Int, goalColumn: Int,
                      limit: Int, gold: Int, silver: Int, layout: String, route: List<Int>, taps: List<Int>, edition: Int = 2): AuthoredLevel {
    val tiles = layout.trim().split(Regex("\\s+")).map { token ->
        require(token.matches(Regex("[ILT][0-3]|[XR]"))) { "Invalid level $id token: $token" }
        Tile(when (token[0]) {
            'I' -> TileKind.STRAIGHT; 'L' -> TileKind.CORNER; 'T' -> TileKind.T_JUNCTION
            'X' -> TileKind.CROSS; else -> TileKind.BLOCKED
        }, token.drop(1).toIntOrNull() ?: 0)
    }
    val definition = LevelDefinition(id, edition, Board(size,size,tiles),
        Endpoint(Cell(-1,sourceColumn),Direction.SOUTH,TileKind.SOURCE),
        Endpoint(Cell(size,goalColumn),Direction.NORTH,TileKind.GOAL),limit,gold,silver)
    return AuthoredLevel(title,definition,route,taps,lesson)
}

object LevelCatalog {
    const val CONTENT_VERSION = 3
    val levels: List<AuthoredLevel> = RestoredCampaign.levels
    fun get(id: Int, version: Int = CONTENT_VERSION): LevelDefinition = when (version) {
        1 -> LegacyLevelCatalog.get(id)
        2 -> ReviewedCampaign.levels.first { it.definition.id == id }.definition
        CONTENT_VERSION -> levels.firstOrNull { it.definition.id == id }?.definition ?: error("Unsupported level $id")
        else -> error("Unsupported content edition $version")
    }
    fun title(id: Int, version: Int = CONTENT_VERSION): String =
        when (version) {
            1 -> LegacyLevelCatalog.title(id)
            2 -> ReviewedCampaign.levels.first { it.definition.id == id }.title
            else -> levels.first { it.definition.id == id }.title
        }
    fun lesson(id: Int) = levels.first { it.definition.id == id }.lesson
}
