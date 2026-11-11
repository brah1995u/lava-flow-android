package com.lavaflow.puzzle.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.lavaflow.puzzle.R
import java.text.NumberFormat

val Gold = Color(0xFFF4D17D)
val Cream = Color(0xFFFFF0CE)
val Muted = Color(0xFFD0BCD6)
val Lava = Color(0xFFFF9B49)
val Deep = Color(0xFF1A1022)
val Purple = Color(0xFF452054)

/** Nine-slice transforms of the supplied artwork; no synthesized panel illustration. */
@Composable
fun SlicedArt(@DrawableRes resource: Int, modifier: Modifier = Modifier, button: Boolean = false) {
    val bitmap = ImageBitmap.imageResource(resource)
    Canvas(modifier) {
        val sourceX = if (button) 150 else 100
        val sourceTop = if (button) 90 else 170
        val sourceBottom = if (button) 90 else 58
        val horizontal = (if (button) 30.dp else 28.dp).toPx().toInt().coerceAtMost(size.width.toInt() / 2)
        val top = (if (button) 12.dp else 64.dp).toPx().toInt().coerceAtMost(size.height.toInt() / 2)
        val bottom = (if (button) 12.dp else 22.dp).toPx().toInt().coerceAtMost(size.height.toInt() / 3)
        val sx = intArrayOf(0, sourceX, bitmap.width - sourceX, bitmap.width)
        val sy = intArrayOf(0, sourceTop, bitmap.height - sourceBottom, bitmap.height)
        val dx = intArrayOf(0, horizontal, size.width.toInt() - horizontal, size.width.toInt())
        val dy = intArrayOf(0, top, size.height.toInt() - bottom, size.height.toInt())
        for (row in 0..2) for (column in 0..2) {
            val destination = IntSize(dx[column + 1] - dx[column], dy[row + 1] - dy[row])
            if (destination.width > 0 && destination.height > 0) drawImage(bitmap,
                srcOffset = IntOffset(sx[column], sy[row]), srcSize = IntSize(sx[column + 1] - sx[column], sy[row + 1] - sy[row]),
                dstOffset = IntOffset(dx[column], dy[row]), dstSize = destination,
                filterQuality = FilterQuality.High)
        }
    }
}

@Composable
fun ArtPanel(title: String, modifier: Modifier = Modifier, @DrawableRes resource: Int = R.drawable.ui_panel_purple_ornate, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxWidth()) {
        SlicedArt(resource, Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(start = 25.dp, end = 25.dp, top = 67.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        Text(title, color = Cream, fontSize = 17.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 16.dp))
    }
}

@Composable
fun GoldAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, tag: String = label) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, label = "goldPress")
    Box(modifier.heightIn(min = 58.dp).graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else .45f }
        .testTag(tag).clickable(interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        SlicedArt(R.drawable.ui_button_gold_wide, Modifier.matchParentSize(), button = true)
        Text(label, color = Color(0xFF42200A), fontSize = 17.sp, fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center, letterSpacing = .8.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp))
    }
}

@Composable
fun SmallLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tag: String = label) {
    Text(label, color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        modifier = modifier.testTag(tag).heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(14.dp))
}

@Composable
fun Wallet(coins: Long, modifier: Modifier = Modifier) {
    Row(modifier.background(Deep.copy(alpha = .84f), RoundedCornerShape(50)).border(1.dp, Gold.copy(alpha = .45f), RoundedCornerShape(50))
        .padding(horizontal = 10.dp, vertical = 7.dp).semantics(mergeDescendants = true) { contentDescription = "$coins coins" }.testTag("wallet"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Image(painterResource(R.drawable.ic_coin_volcano), null, Modifier.size(25.dp))
        Text(NumberFormat.getIntegerInstance().format(coins), color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StarRow(stars: Int, size: Dp = 16.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.semantics { contentDescription = "$stars of 3 stars" }) {
        repeat(3) { index -> Image(painterResource(R.drawable.ic_star_gold), null, Modifier.size(size).alpha(if (index < stars) 1f else .20f)) }
    }
}

@Composable
fun MenuTile(label: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    Column(modifier.testTag("menu_${label.lowercase()}").clickable(interactions, null, role = Role.Button, onClick = onClick)
        .padding(3.dp).graphicsLayer { scaleX = if (pressed) .95f else 1f; scaleY = scaleX }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ui_button_gold_octagonal), null, Modifier.fillMaxSize())
            Image(painterResource(icon), null, Modifier.size(40.dp))
        }
        Text(label, color = Cream, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
    }
}

val TitleStyle = TextStyle(color = Gold, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black,
    shadow = Shadow(Color(0xFF8D340C), androidx.compose.ui.geometry.Offset(0f, 4f), 12f), letterSpacing = 2.sp)
