package com.lavaflow.puzzle.core

/** Independent of board shape: future content uses the same level/efficiency contract. */
data class CompletionReward(val levelCoins: Long, val efficiencyCoins: Long, val firstClearCoins: Long) {
    val total get() = Math.addExact(Math.addExact(levelCoins, efficiencyCoins), firstClearCoins)
    init { require(levelCoins >= 0 && efficiencyCoins >= 0 && firstClearCoins >= 0) }
}

object RewardEconomy {
    fun levelCoins(level: Int): Long { require(level > 0); return 60L + (level - 1L) * 8 }
    fun coinsPerMove(level: Int): Long { require(level > 0); return 4L + (level - 1L) / 10 }
    fun firstClearCoins(level: Int): Long { require(level > 0); return 100L + (level - 1L) * 10 }
    fun completion(level: Int, movesRemaining: Int, firstClear: Boolean): CompletionReward {
        require(movesRemaining >= 0)
        return CompletionReward(levelCoins(level), movesRemaining * coinsPerMove(level),
            if (firstClear) firstClearCoins(level) else 0)
    }
}

data class DailyProgress(val claims: Long = 0, val lastClaimDay: Long? = null) {
    init { require(claims >= 0 && (claims == 0L) == (lastClaimDay == null)) }
}
data class DailyReward(val coins: Long, val boosters: Map<Booster, Int> = emptyMap())
enum class DailyStatus { CLAIMED, ALREADY_CLAIMED }
data class DailyDecision(val profile: PlayerProfile, val status: DailyStatus)

/** The caller supplies today's local calendar epoch-day inside the atomic store transaction.
 * Missed dates do not reset the cycle. Equal or earlier dates cannot grant another claim. */
object DailyRewards {
    val cycle = listOf(
        DailyReward(75, mapOf(Booster.HINT to 1)),
        DailyReward(100, mapOf(Booster.HINT to 1)),
        DailyReward(125),
        DailyReward(150, mapOf(Booster.HAMMER to 1)),
        DailyReward(175, mapOf(Booster.AUTO_ROTATE to 1)),
        DailyReward(200, mapOf(Booster.SWAP to 1)),
        DailyReward(300, Booster.entries.associateWith { 1 })
    )
    fun available(progress: DailyProgress, today: Long) = progress.lastClaimDay?.let { today > it } ?: true
    fun nextDay(progress: DailyProgress) = (progress.claims % cycle.size).toInt() + 1
    fun displayedDay(progress: DailyProgress, today: Long) =
        if (available(progress, today)) nextDay(progress) else ((progress.claims - 1) % cycle.size).toInt() + 1

    fun claim(profile: PlayerProfile, today: Long): DailyDecision {
        if (!available(profile.daily, today)) return DailyDecision(profile, DailyStatus.ALREADY_CLAIMED)
        val reward = cycle[nextDay(profile.daily) - 1]
        var coins = Math.addExact(profile.coins, reward.coins)
        val inventory = profile.inventory.toMutableMap()
        reward.boosters.forEach { (booster, count) ->
            // Full inventory converts that item to its shop value; the reward is never lost.
            if (profile.stock(booster) > Int.MAX_VALUE - count) coins = Math.addExact(coins, booster.price * count)
            else inventory[booster] = profile.stock(booster) + count
        }
        return DailyDecision(profile.copy(coins = coins, inventory = inventory,
            daily = DailyProgress(Math.addExact(profile.daily.claims, 1), today)), DailyStatus.CLAIMED)
    }
}
