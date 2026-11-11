package com.lavaflow.puzzle.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavaflow.puzzle.R
import com.lavaflow.puzzle.core.*

/** Real per-level records from this device. No invented players or online ranks. */
@Composable
fun Leaderboard(profile: PlayerProfile, back: () -> Unit) {
    val records = remember(profile.bestStars, profile.moveRecords, profile.bestMoves) {
        profile.bestStars.keys.sortedWith(compareByDescending<Int> { profile.bestStars.getValue(it) }
            .thenBy { (profile.bestMovesFor(it) ?: Int.MAX_VALUE).toDouble() / LevelCatalog.get(it).moveLimit }
            .thenBy { it })
    }
    Column(Modifier.fillMaxSize().testTag("leaderboard")) {
        PageHeader("LEADERBOARD", profile, back)
        ForgeFrame(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp), 35.dp) {
            Row(Modifier.fillMaxWidth().padding(23.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.orn_trophy), null, Modifier.size(65.dp))
                Column(Modifier.padding(start = 16.dp)) {
                    Text("YOUR BEST FLOWS", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("${profile.totalStars} stars  ·  ${records.size} levels cleared", color = Cream, fontSize = 13.sp)
                    Text("LOCAL RECORDS · THIS DEVICE", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp))
                }
            }
        }
        if (records.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f).padding(35.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Image(painterResource(R.drawable.orn_compass), null, Modifier.size(110.dp))
                Text("Your first flow belongs here", style = TitleStyle, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 18.dp))
                Text("Complete a level to record its stars and best move count.", color = Cream, textAlign = TextAlign.Center)
            }
        } else LazyColumn(Modifier.weight(1f).testTag("leaderboard_list"), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            itemsIndexed(records, key = { _, level -> level }) { rank, level ->
                ForgeFrame(Modifier.fillMaxWidth().testTag("record_$level"), 28.dp) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 19.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${rank+1}", color = Gold, fontSize = 23.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(30.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 7.dp)) {
                            Text("LEVEL $level", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(LevelCatalog.title(level), color = Cream, fontSize = 12.sp)
                            StarRow(profile.bestStars.getValue(level), 14.dp)
                        }
                        Text(profile.bestMovesFor(level)?.let { "$it\nMOVES" } ?: if (profile.bestMovesFor(level,1) != null) "EARLIER\nEDITION" else "—", color = Cream, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Text("Ranked by stars, then moves used as a share of the level limit.", color = Muted, fontSize = 10.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 23.dp, vertical = 10.dp))
    }
}
