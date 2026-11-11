package com.lavaflow.puzzle.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lavaflow.puzzle.R
import com.lavaflow.puzzle.core.*
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

/** A supplied-style illustrated rim, sliced without distorting the sculpted corners. */
@Composable
fun RelicPanel(modifier: Modifier = Modifier, edge: Dp = 15.dp, content: @Composable BoxScope.() -> Unit) {
    val bitmap = ImageBitmap.imageResource(R.drawable.vr_board_frame)
    Box(modifier.clip(RoundedCornerShape(edge))) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF54106B), Color(0xFF290737), Color(0xFF3D104F))),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(edge.toPx()))
            val guide = (bitmap.width * .13f).toInt()
            val dx = minOf(edge.toPx().toInt(), size.width.toInt() / 2, size.height.toInt() / 2)
            val sx = intArrayOf(0, guide, bitmap.width - guide, bitmap.width)
            val sy = intArrayOf(0, guide, bitmap.height - guide, bitmap.height)
            val x = intArrayOf(0, dx, size.width.toInt() - dx, size.width.toInt())
            val y = intArrayOf(0, dx, size.height.toInt() - dx, size.height.toInt())
            for (row in 0..2) for (col in 0..2) {
                if (row == 1 && col == 1) continue
                val width = x[col + 1] - x[col]; val height = y[row + 1] - y[row]
                if (width > 0 && height > 0) drawImage(bitmap, IntOffset(sx[col], sy[row]),
                    IntSize(sx[col + 1] - sx[col], sy[row + 1] - sy[row]), IntOffset(x[col], y[row]),
                    IntSize(width, height), filterQuality = FilterQuality.High)
            }
        }
        content()
    }
}

@Composable
fun rememberForegroundClock(): Float {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var active by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) active = true
            if (event == Lifecycle.Event.ON_PAUSE) active = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (!active) return 0f
    val transition = rememberInfiniteTransition(label = "foregroundEmbers")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "emberDrift")
    return phase
}

@Composable
fun EmberField(modifier: Modifier = Modifier, clock: Float = rememberForegroundClock(), celebratory: Boolean = false) {
    Canvas(modifier) {
        repeat(if (celebratory) 40 else 23) { i ->
            val phase = (clock + i * .137f) % 1f
            val x = ((i * .173f) % 1f) * size.width + sin(phase * PI.toFloat() * 2 + i) * 12.dp.toPx()
            val y = size.height * (1f - phase)
            val opacity = sin(phase * PI.toFloat()).coerceIn(0f, 1f)
            val center = Offset(x, y)
            val radius = (if (i % 3 == 0) 2.2.dp else 1.dp).toPx()
            drawCircle(Color(0xFFFF6500).copy(alpha = opacity * .15f), radius * 3.5f, center)
            drawCircle(Color(0xFFFFDB76).copy(alpha = opacity * .72f), radius, center)
        }
    }
}

@Composable
fun RelicWallet(coins: Long, modifier: Modifier = Modifier) {
    val label = remember(coins) { NumberFormat.getIntegerInstance().format(coins) }
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    RelicPanel(modifier.height(50.dp).width(126.dp).testTag("wallet")
        .semantics(mergeDescendants = true) { contentDescription = "$coins coins" }, edge = 11.dp) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
            val iconSize = if (maxWidth < 100.dp) 24.dp else 34.dp
            val space = with(density) { (maxWidth - iconSize - 4.dp).toPx() }.coerceAtLeast(1f)
            val textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 18.sp)
            val fitted = remember(label, space, density) {
                val width = measurer.measure(label, textStyle, maxLines = 1).size.width.coerceAtLeast(1)
                (18f * minOf(1f, space / width)).sp
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_coin_volcano), null, Modifier.size(iconSize))
                Text(label, color = Cream, style = textStyle, fontSize = fitted, maxLines = 1, softWrap = false,
                    modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
fun Medallion(@DrawableRes icon: Int, label: String, modifier: Modifier = Modifier, tag: String = label, onClick: () -> Unit) {
    Box(modifier.size(54.dp).testTag(tag).clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ui_button_gold_octagonal), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        Box(Modifier.fillMaxSize(.76f).background(Brush.radialGradient(listOf(Color(0xFF75219C), Color(0xFF2E083F))), CircleShape))
        Image(painterResource(icon), null, Modifier.fillMaxSize(.56f))
    }
}

@Composable
fun GameplayHud(round: PuzzleRound, coins: Long, compact: Boolean, pause: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(if (compact) 66.dp else 80.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        RelicWallet(coins, Modifier.weight(1.4f))
        RelicPanel(Modifier.weight(1.05f).fillMaxHeight().padding(vertical = 3.dp), 13.dp) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LEVEL", color = Gold, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(round.levelId.toString(), color = Cream, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = if (compact) 25.sp else 32.sp)
            }
        }
        RelicPanel(Modifier.weight(.95f).height(if (compact) 54.dp else 65.dp).testTag("moves_counter"), 11.dp) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MOVES", color = Gold, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text(round.movesRemaining.toString(), color = if (round.movesRemaining <= 3) Lava else Cream,
                    fontFamily = FontFamily.Serif, fontSize = if (compact) 23.sp else 29.sp, fontWeight = FontWeight.Black)
            }
        }
        Medallion(R.drawable.ic_settings_gold, "Pause", Modifier.size(if (compact) 43.dp else 50.dp), "game_pause", pause)
    }
}

@Composable
fun StarObjective(round: PuzzleRound, modifier: Modifier = Modifier) {
    val available = round.definition.stars(round.moves)
    Box(modifier.height(37.dp).semantics { contentDescription = "Star target: up to $available stars" }) {
        RelicPanel(Modifier.fillMaxWidth().height(14.dp).align(Alignment.Center), 6.dp) {}
        Row(Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 22.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(3) { index -> Image(painterResource(R.drawable.ic_star_gold), null, Modifier.size(if (index == 1) 33.dp else 29.dp).alpha(if (index < available) 1f else .25f)) }
        }
    }
}

@Composable
fun BoosterDock(profile: PlayerProfile, compact: Boolean, ready: Boolean, selected: Booster?,
                modifier: Modifier = Modifier, activate: (Booster) -> Unit) {
    val icons = listOf(R.drawable.vr_hammer, R.drawable.vr_rotate, R.drawable.vr_hint, R.drawable.vr_swap)
    RelicPanel(modifier.height(if (compact) 88.dp else 108.dp), edge = 15.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 9.dp), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            Booster.entries.forEachIndexed { index, booster ->
                val enabled = ready && profile.stock(booster) > 0 && !(booster == Booster.HINT && profile.activeRound?.effect?.booster == Booster.HINT)
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) .92f else 1f, tween(100), label = "boosterPress")
                Column(Modifier.weight(1f).testTag("booster_${booster.name.lowercase()}").semantics(mergeDescendants = true) {
                    contentDescription = "${booster.title}, ${profile.stock(booster)} in inventory"
                    stateDescription = when { booster == selected -> "Selected"; !enabled -> "Unavailable"; else -> "Ready" }
                }.clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button) { activate(booster) },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(if (compact) 53.dp else 68.dp)) {
                        if (selected == booster) Box(Modifier.fillMaxSize().background(Color(0x777C2FA1), CircleShape).border(2.dp, Gold, CircleShape))
                        Image(painterResource(icons[index]), null, Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }.alpha(if (enabled || selected == booster) 1f else .42f))
                        Box(Modifier.size(if (compact) 23.dp else 27.dp).align(Alignment.BottomEnd).clip(CircleShape), contentAlignment = Alignment.Center) {
                            Image(painterResource(R.drawable.ui_button_gold_octagonal), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                            Box(Modifier.fillMaxSize(.74f).background(Color(0xFF491063), CircleShape))
                            Text(profile.stock(booster).toString(), color = Cream, fontSize = 13.sp, fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    Text(if (booster == Booster.AUTO_ROTATE) "ROTATE" else booster.title.uppercase(), color = Gold.copy(alpha = .78f),
                        fontSize = 8.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
                }
            }
        }
    }
}

@Composable
fun CelebrationStars(stars: Int, clock: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width / 2, size.height * .65f)
            drawCircle(Brush.radialGradient(listOf(Color(0xAAFFB626), Color(0x33FF5A00), Color.Transparent), center, size.width * .48f),
                size.width * .48f, center)
            repeat(14) { i ->
                val angle = i * PI.toFloat() * 2 / 14 + clock * .12f
                val radius = size.width * .47f
                val ray = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + cos(angle - .025f) * radius, center.y + sin(angle - .025f) * radius)
                    lineTo(center.x + cos(angle + .025f) * radius, center.y + sin(angle + .025f) * radius); close()
                }
                drawPath(ray, Color(0xFFFFCE63).copy(alpha = .10f))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.semantics { contentDescription = "$stars of 3 stars" }) {
            repeat(3) { index ->
                // The shared foreground clock pauses with the activity; no extra animation loop.
                val reveal = if (index < stars) 1f + sin(clock * 2.4f - index * .65f) * .035f else 1f
                Image(painterResource(R.drawable.ic_star_gold), null, Modifier.size(if (index == 1) 83.dp else 63.dp)
                    .offset(y = if (index == 1) (-7).dp else 6.dp)
                    .graphicsLayer { scaleX = reveal; scaleY = reveal }
                    .alpha(if (index < stars) 1f else .23f))
            }
        }
    }
}

@Composable
fun PremiumHome(
    profile: PlayerProfile,
    go: (String) -> Unit,
    titleArtwork: androidx.compose.ui.graphics.painter.Painter,
    volcanoArtwork: androidx.compose.ui.graphics.painter.Painter,
    today: Long,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 700.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 17.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                RelicWallet(profile.coins)
                DailyHomeEntry(profile, today, Modifier.width(150.dp)) { go("daily") }
            }
            ClassicVolcanicHero(titleArtwork, volcanoArtwork, compact,
                Modifier.fillMaxWidth().height(if (compact) 170.dp else 310.dp), tag = "classic_home_hero")
            GoldAction("PLAY", { go("levels") }, Modifier.fillMaxWidth(.95f).height(if (compact) 56.dp else 70.dp), tag = "play")
            Text("${profile.bestStars.size} / ${GameCatalog.LEVEL_SLOTS} LEVELS  ·  ${profile.totalStars} STARS", color = Gold, fontSize = 10.sp,
                letterSpacing = 1.sp, modifier = Modifier.padding(vertical = if (compact) 5.dp else 7.dp))
            val entries = listOf(Triple("SHOP", R.drawable.ic_shop_gold, "shop"),
                Triple("LEADERBOARD", R.drawable.orn_trophy, "leaderboard"),
                Triple("ACHIEVEMENTS", R.drawable.orn_compass, "achievements"),
                Triple("SETTINGS", R.drawable.ic_settings_gold, "settings"))
            entries.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(bottom = if (compact) 3.dp else 5.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    pair.forEach { (label, icon, route) ->
                        ForgeFrame(Modifier.weight(1f).height(if (compact) 58.dp else 74.dp), edge = 30.dp) {
                            if (compact) {
                                CompactHomeAction(label, icon, "menu_$route") { go(route) }
                            } else {
                                IconAction(label, icon, "menu_$route", Modifier.fillMaxSize().padding(horizontal = 10.dp)) { go(route) }
                            }
                        }
                    }
                }
            }
            ForgeFrame(Modifier.fillMaxWidth().height(if (compact) 58.dp else 65.dp), edge = 27.dp) {
                Row(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(Triple("ABOUT", R.drawable.orn_info, "about"), Triple("PRIVACY", R.drawable.orn_privacy, "privacy"),
                        Triple("LEGAL", R.drawable.orn_legal, "legal")).forEach { (label, icon, route) ->
                        Column(Modifier.weight(1f).fillMaxHeight().testTag(route).clickable(role = Role.Button) { go(route) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Image(painterResource(icon), null, Modifier.size(27.dp))
                            Text(label, color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Give long navigation labels their own full-width line on compact portrait screens. */
@Composable
private fun CompactHomeAction(label: String, @DrawableRes icon: Int, tag: String, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .94f else 1f, label = "compactHomeAction")
    Column(Modifier.fillMaxSize().testTag(tag).semantics(mergeDescendants = true) { contentDescription = label }
        .clickable(interactions, null, role = Role.Button, onClick = onClick)
        .graphicsLayer { scaleX = scale; scaleY = scale }.padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(icon), null, Modifier.size(25.dp))
        Text(label, color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp,
            maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 3.dp))
    }
}
