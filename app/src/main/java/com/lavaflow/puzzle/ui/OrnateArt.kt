package com.lavaflow.puzzle.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.lavaflow.puzzle.R
import com.lavaflow.puzzle.core.*
import kotlin.math.PI
import kotlin.math.sin

val LocalHomeAction = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun OutcomeCrest(won: Boolean, stars: Int, clock: Float) {
    val compact = LocalConfiguration.current.screenHeightDp < 760
    val shortScreen = LocalConfiguration.current.screenHeightDp < 600
    Box(Modifier.fillMaxWidth().height(if (shortScreen) 104.dp else if (compact) 160.dp else 195.dp), contentAlignment = Alignment.TopCenter) {
        Image(painterResource(if (won) R.drawable.orn_win else R.drawable.orn_lose), null,
            Modifier.fillMaxWidth().height(if (shortScreen) 89.dp else if (compact) 145.dp else 180.dp))
        if (won) Row(Modifier.align(Alignment.BottomCenter).semantics { contentDescription = "$stars of 3 stars" },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                val pulse = if (index < stars) 1f + sin(clock * 2f * PI.toFloat() - index) * .04f else 1f
                Image(painterResource(R.drawable.ic_star_gold), null, Modifier.size(if (index == 1) 64.dp else 49.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = if (index < stars) 1f else .23f })
            }
        }
    }
}

/** Twenty-five slices preserve corner compasses AND center gems; only plain rails stretch. */
@Composable
fun ForgeFrame(modifier: Modifier = Modifier, edge: Dp = 42.dp, subdued: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val art = ImageBitmap.imageResource(R.drawable.orn_frame)
    Box(modifier) {
        Box(Modifier.matchParentSize().padding(9.dp).background(Brush.verticalGradient(
            if (subdued) listOf(Color(0xFF272031), Color(0xFF15101D), Color(0xFF35202A))
            else listOf(Color(0xFF4F156B), Color(0xFF230B35), Color(0xFF3E1054))), RoundedCornerShape(12.dp)))
        Canvas(Modifier.matchParentSize()) {
            val e = minOf(edge.toPx(), size.width / 3, size.height / 3).toInt()
            val gem = minOf(14.dp.toPx().toInt(), e / 2)
            val s = intArrayOf(0, (art.width * .28f).toInt(), (art.width * .44f).toInt(), (art.width * .56f).toInt(), (art.width * .72f).toInt(), art.width)
            val x = intArrayOf(0, e, size.width.toInt()/2-gem, size.width.toInt()/2+gem, size.width.toInt()-e, size.width.toInt())
            val y = intArrayOf(0, e, size.height.toInt()/2-gem, size.height.toInt()/2+gem, size.height.toInt()-e, size.height.toInt())
            for (row in 0..4) for (col in 0..4) {
                if (row in 1..3 && col in 1..3) continue
                val w = x[col+1]-x[col]; val h = y[row+1]-y[row]
                if (w > 0 && h > 0) drawImage(art, IntOffset(s[col],s[row]), IntSize(s[col+1]-s[col],s[row+1]-s[row]),
                    IntOffset(x[col],y[row]), IntSize(w,h), alpha = if (subdued) .65f else 1f, filterQuality = FilterQuality.High)
            }
        }
        content()
    }
}

@Composable
fun IconAction(label: String, @DrawableRes icon: Int, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .94f else 1f, label = "iconAction")
    Row(modifier.heightIn(min = 48.dp).testTag(tag).semantics(mergeDescendants = true) { contentDescription = label }
        .clickable(interactions, null, role = Role.Button, onClick = onClick)
        .graphicsLayer { scaleX = scale; scaleY = scale }, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
        Image(painterResource(icon), null, Modifier.size(29.dp))
        Text(label, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp), textAlign = TextAlign.Center)
    }
}

@DrawableRes fun boosterArt(booster: Booster): Int = when (booster) {
    Booster.HAMMER -> R.drawable.vr_hammer; Booster.AUTO_ROTATE -> R.drawable.vr_rotate
    Booster.HINT -> R.drawable.vr_hint; Booster.SWAP -> R.drawable.vr_swap
}

@DrawableRes fun achievementArt(achievement: Achievement): Int = when (achievement) {
    Achievement.FIRST_FLOW -> R.drawable.ic_coin_volcano
    Achievement.PERFECT_FLOW -> R.drawable.ic_star_gold
    Achievement.FLOW_ENGINEER -> R.drawable.orn_compass
    Achievement.MASTER_ENGINEER -> R.drawable.ic_settings_gold
    Achievement.NO_HELP_NEEDED -> R.drawable.orn_privacy
    Achievement.ROCK_BREAKER -> R.drawable.vr_hammer
    Achievement.PATHFINDER -> R.drawable.vr_hint
    Achievement.LAVA_MASTER -> R.drawable.vr_volcano
    Achievement.GRAND_FLOW -> R.drawable.orn_crown
    Achievement.PERFECTION -> R.drawable.orn_trophy
}

/** Registers the existing illustrated outlet to a tile-width channel without shifting its logical source. */
@Composable
fun RegisteredVolcano(channelWidth: Dp, modifier: Modifier = Modifier) {
    val art = ImageBitmap.imageResource(R.drawable.vr_volcano)
    Canvas(modifier.semantics { contentDescription = "Volcano source" }) {
        // Bright outlet spans x250..286; the tile's lava bed spans roughly52/256 of its width.
        val sx = intArrayOf(0, 244, 292, art.width)
        val half = channelWidth.toPx() / 2
        val dx = intArrayOf(0, (size.width/2-half).toInt(), (size.width/2+half).toInt(), size.width.toInt())
        for (i in 0..2) drawImage(art, IntOffset(sx[i],0), IntSize(sx[i+1]-sx[i],art.height),
            IntOffset(dx[i],0), IntSize(dx[i+1]-dx[i],size.height.toInt()), filterQuality = FilterQuality.High)
    }
}
