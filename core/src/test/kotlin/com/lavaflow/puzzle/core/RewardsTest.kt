package com.lavaflow.puzzle.core

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RewardsTest {
    @Test fun rewardsExplainEveryCoinIncludingReplaysAndLastMoveWins() {
        assertEquals(CompletionReward(60,24,100), RewardEconomy.completion(1,6,true))
        assertEquals(84L, RewardEconomy.completion(1,6,false).total)
        assertEquals(60L, RewardEconomy.completion(1,0,false).total)
        assertEquals(CompletionReward(292,36,390), RewardEconomy.completion(30,6,true))
        assertEquals(718L, RewardEconomy.completion(30,6,true).total)
    }
    @Test fun levelAndEfficiencyRewardsScaleBeyondCurrentContent() {
        for (level in 1..1000) for (left in 0..20) {
            assertTrue(RewardEconomy.completion(level+1,left,false).total > RewardEconomy.completion(level,left,false).total)
            assertTrue(RewardEconomy.completion(level,left+1,false).total > RewardEconomy.completion(level,left,false).total)
            assertTrue(RewardEconomy.completion(level,left,false).total > 0)
        }
    }
    @Test fun sevenClaimsCreditExactCoinsAndAllFourBoosterKindsThenCycle() {
        var p=PlayerProfile(0)
        for(day in 1L..7L) {
            val result=DailyRewards.claim(p,day)
            assertEquals(DailyStatus.CLAIMED,result.status)
            p=result.profile
            assertEquals(day.toInt(),DailyRewards.displayedDay(p.daily,day))
        }
        assertEquals(1125L,p.coins)
        assertEquals(mapOf(Booster.HAMMER to 2,Booster.AUTO_ROTATE to 2,Booster.HINT to 3,Booster.SWAP to 2),p.inventory)
        assertEquals(1,DailyRewards.nextDay(p.daily))
        p=DailyRewards.claim(p,8).profile
        assertEquals(1200L,p.coins);assertEquals(4,p.stock(Booster.HINT))
    }
    @Test fun missedDaysDoNotResetOrGrantTheSkippedGifts() {
        val first=DailyRewards.claim(PlayerProfile(10),10).profile
        val later=DailyRewards.claim(first,100).profile
        assertEquals(185L,later.coins);assertEquals(2L,later.daily.claims)
        assertEquals(2,later.stock(Booster.HINT))
    }
    @Test fun duplicateAndBackwardsCalendarDaysNeverClaimTwice() {
        val p=DailyRewards.claim(PlayerProfile(0),100).profile
        for(day in listOf(100L,100L,99L,1L)) {
            val result=DailyRewards.claim(p,day)
            assertEquals(DailyStatus.ALREADY_CLAIMED,result.status);assertEquals(p,result.profile)
        }
        assertTrue(DailyRewards.available(p.daily,101))
    }
    @Test fun localCalendarWorksAcrossLeapDayMonthAndYearBoundaries() {
        for(date in listOf("2024-02-28","2024-02-29","2026-12-31")) {
            val today=LocalDate.parse(date)
            val claimed=DailyRewards.claim(PlayerProfile(0),today.toEpochDay()).profile
            assertTrue(DailyRewards.available(claimed.daily,today.plusDays(1).toEpochDay()))
        }
    }
    @Test fun fullBoosterInventoryConvertsGiftToItsShopValue() {
        val p=PlayerProfile(0,inventory=Booster.entries.associateWith { Int.MAX_VALUE })
        val next=DailyRewards.claim(p,10).profile
        assertEquals(175L,next.coins);assertEquals(p.inventory,next.inventory)
    }
    @Test fun overflowingWalletCannotPartiallyApplyAGift() {
        val p=PlayerProfile(Long.MAX_VALUE)
        assertTrue(runCatching { DailyRewards.claim(p,1) }.isFailure)
        assertEquals(DailyProgress(),p.daily);assertEquals(0,p.stock(Booster.HINT))
    }
}
