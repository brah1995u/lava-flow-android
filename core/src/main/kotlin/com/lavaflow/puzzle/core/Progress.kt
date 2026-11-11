package com.lavaflow.puzzle.core

/** Presentation slots remain separate from implemented, playable content. */
object GameCatalog {
    val LEVEL_SLOTS get() = LevelCatalog.levels.size
    const val FIRST_GAMEPLAY_SLICE = 5
    val playableLevels = (1..LEVEL_SLOTS).toSet()
}

enum class Booster(val title: String, val price: Long, val description: String) {
    HAMMER("HAMMER", 150, "Remove one blocked rock tile."),
    AUTO_ROTATE("AUTO ROTATE", 200, "Turn one useful tile toward the solution."),
    HINT("HINT", 100, "Reveal 2–3 useful tiles along your path."),
    SWAP("SWAP", 250, "Exchange two adjacent playable tiles.")
}

data class FeedbackSettings(val music: Boolean = true, val sound: Boolean = true, val vibration: Boolean = true)
enum class Setting { MUSIC, SOUND, VIBRATION }

data class PlayerProfile(
    val coins: Long,
    val inventory: Map<Booster, Int> = Booster.entries.associateWith { 0 },
    val unlockedLevel: Int = 1,
    val bestStars: Map<Int, Int> = emptyMap(),
    val bestMoves: Map<Int, Int> = emptyMap(),
    val rewardClaims: Map<Int, Long> = emptyMap(),
    val activeRound: PuzzleRound? = null,
    val unassistedLevels: Set<Int> = emptySet(),
    val hammerUses: Int = 0,
    val hintUses: Int = 0,
    val unlockedAchievements: Set<String> = emptySet(),
    val completionEvents: Set<String> = emptySet(),
    val boosterEvents: Set<String> = emptySet(),
    val recentPurchases: List<String> = emptyList(),
    val settings: FeedbackSettings = FeedbackSettings(),
    val tutorialSeen: Set<String> = emptySet(),
    val moveRecords: Map<String, Int> = emptyMap(),
    val daily: DailyProgress = DailyProgress()
) {
    init {
        require(coins >= 0)
        require(unlockedLevel in 1..GameCatalog.LEVEL_SLOTS)
        require(inventory.keys == Booster.entries.toSet() && inventory.values.all { it >= 0 })
        require(bestStars.all { (level, stars) -> level in 1..GameCatalog.LEVEL_SLOTS && level <= unlockedLevel && stars in 1..3 })
        require(bestMoves.all { (level, moves) -> level in bestStars && moves >= 0 })
        require(rewardClaims.all { (level, coins) -> level in bestStars && coins >= 0 })
        require(unassistedLevels.all { it in bestStars })
        require(hammerUses >= 0 && hintUses >= 0)
        require(moveRecords.all { (key, moves) ->
            val parts = key.split(':')
            parts.size == 2 && parts[0].toIntOrNull() in 1..LevelCatalog.CONTENT_VERSION &&
                parts[1].toIntOrNull() in bestStars && moves >= 0
        })
    }
    val totalStars get() = bestStars.values.sum()
    fun stock(booster: Booster) = inventory.getValue(booster)
    fun bestMovesFor(level: Int, version: Int = LevelCatalog.CONTENT_VERSION): Int? =
        moveRecords["$version:$level"] ?: if (version == 1) bestMoves[level] else null
    fun withSetting(setting: Setting, enabled: Boolean) = copy(settings = when (setting) {
        Setting.MUSIC -> settings.copy(music = enabled)
        Setting.SOUND -> settings.copy(sound = enabled)
        Setting.VIBRATION -> settings.copy(vibration = enabled)
    })
}

enum class Achievement(val title: String, val description: String, val target: Int) {
    FIRST_FLOW("FIRST FLOW", "Complete the first level.", 1),
    PERFECT_FLOW("PERFECT FLOW", "Earn 3 stars on a level.", 1),
    FLOW_ENGINEER("FLOW ENGINEER", "Complete 5 levels.", 5),
    MASTER_ENGINEER("MASTER ENGINEER", "Earn 30 total stars.", 30),
    NO_HELP_NEEDED("NO HELP NEEDED", "Complete 3 levels without a booster.", 3),
    ROCK_BREAKER("ROCK BREAKER", "Use Hammer 10 times.", 10),
    PATHFINDER("PATHFINDER", "Use Hint 10 times.", 10),
    LAVA_MASTER("LAVA MASTER", "Complete 20 levels.", 20),
    GRAND_FLOW("GRAND FLOW", "Complete all 30 levels.", 30),
    PERFECTION("PERFECTION", "Earn 90 total stars.", 90);

    fun progress(profile: PlayerProfile): Int = when (this) {
        FIRST_FLOW -> if (1 in profile.bestStars) 1 else 0
        PERFECT_FLOW -> if (profile.bestStars.values.any { it == 3 }) 1 else 0
        FLOW_ENGINEER, LAVA_MASTER, GRAND_FLOW -> profile.bestStars.size
        MASTER_ENGINEER, PERFECTION -> profile.totalStars
        NO_HELP_NEEDED -> profile.unassistedLevels.size
        ROCK_BREAKER -> profile.hammerUses
        PATHFINDER -> profile.hintUses
    }.coerceAtMost(target)
}

enum class PurchaseStatus { PURCHASED, INSUFFICIENT_COINS, INVENTORY_FULL, ALREADY_APPLIED }
data class PurchaseDecision(val profile: PlayerProfile, val status: PurchaseStatus)

object ProgressRules {
    fun purchase(profile: PlayerProfile, booster: Booster, operationId: String): PurchaseDecision {
        require(operationId.isNotBlank())
        if (operationId in profile.recentPurchases) return PurchaseDecision(profile, PurchaseStatus.ALREADY_APPLIED)
        if (profile.coins < booster.price) return PurchaseDecision(profile, PurchaseStatus.INSUFFICIENT_COINS)
        if (profile.stock(booster) == Int.MAX_VALUE) return PurchaseDecision(profile, PurchaseStatus.INVENTORY_FULL)
        return PurchaseDecision(profile.copy(
            coins = profile.coins - booster.price,
            inventory = profile.inventory + (booster to profile.stock(booster) + 1),
            recentPurchases = (profile.recentPurchases + operationId).takeLast(128)
        ), PurchaseStatus.PURCHASED)
    }

    /** Accept only the authoritative connected round after its lava-arrival presentation. */
    fun finishFlow(profile: PlayerProfile, runId: String, revision: Int): PlayerProfile {
        val round = profile.activeRound ?: return profile
        if (round.runId != runId || round.revision != revision || round.paused || round.phase != RoundPhase.FLOWING || !round.connectivity.goalConnected) return profile
        val stars = round.stars
        val level = round.levelId
        val breakdown = RewardEconomy.completion(level, round.movesRemaining, level !in profile.bestStars)
        val reward = breakdown.total
        val next = profile.copy(
            coins = Math.addExact(profile.coins, reward),
            unlockedLevel = maxOf(profile.unlockedLevel, minOf(level + 1, GameCatalog.LEVEL_SLOTS)),
            bestStars = profile.bestStars + (level to maxOf(profile.bestStars[level] ?: 0, stars)),
            bestMoves = profile.bestMoves + (level to minOf(profile.bestMoves[level] ?: Int.MAX_VALUE, round.moves)),
            moveRecords = profile.moveRecords + ("${round.contentVersion}:$level" to
                minOf(profile.bestMovesFor(level, round.contentVersion) ?: Int.MAX_VALUE, round.moves)),
            rewardClaims = profile.rewardClaims + (level to Math.addExact(profile.rewardClaims[level] ?: 0, reward)),
            unassistedLevels = if (round.boostersUsed.isEmpty()) profile.unassistedLevels + level else profile.unassistedLevels,
            activeRound = round.copy(phase = RoundPhase.WON, awardedCoins = reward, rewardBreakdown = breakdown),
            tutorialSeen = if (level == 1) profile.tutorialSeen + setOf("rotate", "connect") else profile.tutorialSeen
        )
        return updateAchievements(next)
    }

    internal fun updateAchievements(profile: PlayerProfile) = profile.copy(
        unlockedAchievements = profile.unlockedAchievements + Achievement.entries
            .filter { it.progress(profile) >= it.target }.map { it.name }
    )
}
