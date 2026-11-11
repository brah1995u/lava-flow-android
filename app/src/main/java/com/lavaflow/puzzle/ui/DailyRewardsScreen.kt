package com.lavaflow.puzzle.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.lavaflow.puzzle.R
import com.lavaflow.puzzle.core.*
import kotlinx.coroutines.delay
import java.time.LocalDate

/** The store checks the date again inside the claim transaction. */
@Composable internal fun rememberRewardDay(): Long {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val day by produceState(LocalDate.now().toEpochDay(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { value = LocalDate.now().toEpochDay(); delay(1_000) }
        }
    }
    return day
}

@Composable internal fun DailyHomeEntry(profile: PlayerProfile, today: Long, modifier: Modifier = Modifier, open: () -> Unit) {
    val ready = DailyRewards.available(profile.daily, today)
    ForgeFrame(modifier.height(56.dp).testTag("menu_daily")
        .semantics(mergeDescendants = true) { contentDescription = if (ready) "Daily reward ready to collect" else "Daily reward collected" }
        .clickable(role = Role.Button, onClick = open), 25.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(painterResource(R.drawable.orn_crown), null, Modifier.size(25.dp))
            Column(Modifier.weight(1f)) {
                Text("DAILY GIFT", color = Gold, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Black)
                Text(if (ready) "Day ${DailyRewards.nextDay(profile.daily)} · Collect" else "Collected", color = Cream, fontSize = 10.sp, lineHeight = 13.sp)
            }
        }
    }
}

@Composable internal fun DailyRewardsScreen(profile: PlayerProfile, today: Long, busy: Boolean, back: () -> Unit, claim: () -> Unit) {
    val ready = DailyRewards.available(profile.daily, today)
    val displayedDay = DailyRewards.displayedDay(profile.daily, today)
    val reward = DailyRewards.cycle[displayedDay - 1]
    Column(Modifier.fillMaxSize().testTag("daily_screen")) {
        PageHeader("THE DAILY EMBER", profile, back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 19.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Image(painterResource(R.drawable.orn_crown), null, Modifier.size(66.dp))
                Column {
                    Text(if (ready) "DAY $displayedDay IS READY" else "DAY $displayedDay COLLECTED", color = Gold,
                        fontWeight = FontWeight.Black, fontSize = 17.sp, modifier = Modifier.testTag("daily_status"))
                    Text("+${reward.coins} COINS", color = Cream, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (reward.boosters.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                reward.boosters.forEach { (booster, count) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(boosterArt(booster)), null, Modifier.size(44.dp))
                        Text("+$count ${booster.title}", color = Gold, fontSize = 9.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            Text("A gift each day you return. Miss a day? Your place in the 7-day cycle stays saved.", color = Cream, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
            DailyRewards.cycle.withIndex().toList().chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (index, gift) ->
                        val day = index + 1
                        val collected = day < displayedDay || !ready && day == displayedDay
                        ForgeFrame(Modifier.weight(1f).height(122.dp).alpha(if (collected) .65f else 1f).testTag("daily_day_$day"), edge = 19.dp) {
                            Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("DAY $day", color = Gold, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Black)
                                Image(painterResource(if (day == 7) R.drawable.orn_crown else gift.boosters.keys.firstOrNull()?.let(::boosterArt) ?: R.drawable.ic_coin_volcano), null, Modifier.size(31.dp))
                                Text("${gift.coins}", color = Cream, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
                                Text(if (collected) "COLLECTED" else if (gift.boosters.isEmpty()) "COINS" else "+${gift.boosters.values.sum()} BOOST${if (day==7) "S" else ""}", color = Gold, fontSize = 8.sp, lineHeight = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
            Text("Day 7 includes all four boosters. Then the cycle begins again.\nOne claim per calendar day on this device.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
        }
        GoldAction(if (ready) "COLLECT DAY $displayedDay" else "COLLECTED · COME BACK TOMORROW", claim,
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), enabled = ready && !busy, tag = "daily_claim")
    }
}
