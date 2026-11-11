package com.lavaflow.puzzle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lavaflow.puzzle.FeedbackController
import com.lavaflow.puzzle.FeedbackEvent
import com.lavaflow.puzzle.BoosterSelection
import com.lavaflow.puzzle.R
import com.lavaflow.puzzle.ShellState
import com.lavaflow.puzzle.ShellViewModel
import com.lavaflow.puzzle.core.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// One BFS depth starts only after the previous tile's reveal has traversed its channel.
private const val CHANNEL_TRAVEL_MS = 120

@Composable
fun GameplayScreen(state: ShellState, model: ShellViewModel, feedback: FeedbackController, home: () -> Unit, levels: () -> Unit,
                   settings: () -> Unit, shop: () -> Unit) {
    val profile = state.profile ?: return
    val round = profile.activeRound
    if (round == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Opening your path…", color = Cream) }
        return
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) foreground = true
            if (event == Lifecycle.Event.ON_PAUSE) foreground = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val running = foreground && !round.paused
    var showRules by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { model.cancelBooster() } }
    val graph = remember(round.board) { round.connectivity }
    val arrival = remember(round.runId) { Animatable(0f) }
    var arrivalReady by remember(round.runId) { mutableStateOf(false) }
    LaunchedEffect(round.runId, round.phase, round.paused, foreground) {
        if (round.phase == RoundPhase.FLOWING && running) {
            arrivalReady = false
            arrival.snapTo(0f)
            val travelMs = maxOf(1600, ((graph.depths.values.maxOrNull() ?: 0) + 1) * CHANNEL_TRAVEL_MS + 250)
            arrival.animateTo(1f, tween(travelMs, easing = LinearEasing))
            delay(500) // Portal bloom finishes before the authoritative result transaction.
            arrivalReady = true
        } else if (round.phase == RoundPhase.WON) arrival.snapTo(1f)
        else if (round.phase == RoundPhase.PLAYING) { arrivalReady = false; arrival.snapTo(0f) }
    }
    // Re-attempt after a transient busy write. Phase/run/revision validation makes repeated callbacks harmless.
    LaunchedEffect(arrivalReady, state.busy, round.phase, round.paused, foreground) {
        if (arrivalReady && !state.busy && running && round.phase == RoundPhase.FLOWING) model.finishFlow(round.runId, round.revision)
    }
    LaunchedEffect(round.runId, round.phase) {
        if (foreground && round.phase == RoundPhase.WON) feedback.play(FeedbackEvent.WIN)
        if (foreground && round.phase == RoundPhase.LOST) feedback.play(FeedbackEvent.LOSE)
    }
    var previousReach by remember(round.runId) { mutableIntStateOf(graph.reachable.size) }
    LaunchedEffect(round.revision, graph.reachable.size) {
        if (running && graph.reachable.size > previousReach) feedback.play(FeedbackEvent.FLOW)
        previousReach = graph.reachable.size
    }
    fun pause() { feedback.click(); model.pauseGame(true) }
    BackHandler { if (round.phase == RoundPhase.WON || round.phase == RoundPhase.LOST) levels() else model.pauseGame(!round.paused) }

    val clock = animatedLavaClock(running && round.phase != RoundPhase.LOST)
    BoxWithConstraints(Modifier.fillMaxSize().testTag("gameplay")) {
        val compact = maxHeight < 700.dp
        val lessonKey = "lesson_v${round.contentVersion}_${round.levelId}"
        val teaching = if (round.phase != RoundPhase.PLAYING) null
            else if (round.levelId == 1 && "rotate" !in profile.tutorialSeen) "rotate"
            else if (round.levelId == 1 && "connect" !in profile.tutorialSeen) "connect"
            else if (round.contentVersion == LevelCatalog.CONTENT_VERSION && round.levelId in setOf(2,3,4,5,7,13) && lessonKey !in profile.tutorialSeen) lessonKey else null
        val instruction = when {
            state.planning != null -> "Finding a useful path…"
            round.phase == RoundPhase.FLOWING -> "Connected · Lava is reaching the portal…"
            state.selection?.booster == Booster.HAMMER -> "HAMMER · Tap a glowing rock"
            state.selection?.booster == Booster.SWAP -> if (state.selection.first == null) "SWAP · Choose the first channel" else "SWAP · Choose a glowing neighbor"
            round.effect?.booster == Booster.HINT -> "HINT · Follow the glowing channels"
            teaching == "rotate" -> "Tap the glowing channel to rotate"
            teaching == "connect" -> "Connect the lava to the portal"
            teaching == lessonKey -> LevelCatalog.lesson(round.levelId)
            round.contentVersion < LevelCatalog.CONTENT_VERSION -> "Saved earlier puzzle · Restart in Pause for the new layout"
            else -> null
        }
        Column(Modifier.fillMaxSize()) {
            GameplayHud(round, profile.coins, compact, ::pause)
            if (instruction == null) StarObjective(round, Modifier.fillMaxWidth().padding(horizontal = 36.dp))
            else InstructionStrip(instruction, if (state.selection != null || state.planning != null) "Cancel" else if (teaching != null) "Got it" else null) {
                if (state.selection != null || state.planning != null) model.cancelBooster() else teaching?.let(model::markTutorial)
            }
            BoardScene(round, graph, clock, arrival.value, running, !state.busy,
                state.selection, teaching == "rotate", Modifier.weight(1f).fillMaxWidth().padding(horizontal = 7.dp)) { model.tapTile(it, feedback::play) }
            BoosterDock(profile, compact, !state.busy && running && round.phase == RoundPhase.PLAYING,
                state.selection?.booster ?: state.planning, Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 3.dp)) {
                model.selectBooster(it, feedback::play)
            }
        }
    }

    when {
        showRules -> GameDialog("HOW LAVA FLOWS", { showRules = false }, "rules_dialog") {
            Image(painterResource(R.drawable.orn_info), null, Modifier.size(48.dp))
            Text("ONE CONTINUOUS PATH", color = Gold, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text("Connect the volcano to the portal. Any correctly connected route wins. Spare pipes and stopped side branches are allowed.", color = Cream, fontSize = 14.sp, lineHeight = 20.sp)
            Text("Tap a straight, bend or T-junction to turn it 90° clockwise for one move. A cross joins all four sides and needs no rotation. Rocks stay fixed.", color = Cream, fontSize = 14.sp, lineHeight = 20.sp)
            Text("Only facing openings connect. Lava enters a tile, then spreads into its arms. A rock, border or mismatched opening stops that branch.", color = Cream, fontSize = 14.sp, lineHeight = 20.sp)
            Text("Hammer opens a rock. Auto Rotate fixes one useful tile. Hint marks 2–3 route tiles. Swap exchanges adjacent channels. Each accepted tool uses one item and no move; Cancel is free.", color = Cream, fontSize = 14.sp, lineHeight = 20.sp)
            Text("Reach the portal before your moves run out. A connection on the last move wins. There is no time limit. Pause keeps your attempt.", color = Cream, fontSize = 14.sp, lineHeight = 20.sp)
            Text("Every clear earns coins: ${RewardEconomy.levelCoins(round.levelId)} for this level + ${RewardEconomy.coinsPerMove(round.levelId)} per move left. The first clear adds ${RewardEconomy.firstClearCoins(round.levelId)}. Replays and booster-assisted wins earn rewards too.", color = Cream, fontSize = 14.sp, lineHeight = 20.sp)
            Text(if (round.contentVersion != LevelCatalog.CONTENT_VERSION) "This saved attempt uses an earlier layout. Finish it, or choose Restart in Pause for the updated board. Your inventory and earned progress stay saved."
                else LevelCatalog.lesson(round.levelId), color = Gold, fontSize = 12.sp, lineHeight = 17.sp)
            GoldAction("BACK TO PAUSE", { showRules = false }, Modifier.fillMaxWidth(), tag = "rules_back")
        }
        round.paused && round.phase in setOf(RoundPhase.PLAYING, RoundPhase.FLOWING) -> GameDialog("PAUSED", { model.pauseGame(false) }, "pause_dialog") {
            Image(painterResource(R.drawable.orn_compass), null, Modifier.size(58.dp))
            Text("Your path is waiting", color = Gold, fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("${round.movesRemaining} moves remain. Pick up where you left off.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            GoldAction("RESUME", { feedback.click(); model.pauseGame(false) }, Modifier.fillMaxWidth(), enabled = !state.busy, tag = "game_resume")
            GoldAction("RESTART", { feedback.click(); model.restartGame() }, Modifier.fillMaxWidth(), enabled = !state.busy, tag = "game_restart")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconAction("Settings", R.drawable.ic_settings_gold, "game_settings", onClick = settings)
                IconAction("Shop", R.drawable.ic_shop_gold, "game_shop", onClick = shop)
                IconAction("Rules", R.drawable.orn_info, "game_rules") { showRules = true }
            }
            IconAction("Home", R.drawable.ic_home_gold, "game_home", Modifier.align(Alignment.CenterHorizontally), home)
        }
        round.phase == RoundPhase.WON -> GameDialog(if (round.levelId == GameCatalog.LEVEL_SLOTS) "MASTER OF THE FLOW" else "FLOW COMPLETE", levels, "win_dialog") {
            OutcomeCrest(true, round.stars, clock)
            Text(if (round.levelId == GameCatalog.LEVEL_SLOTS) "${profile.totalStars} / ${GameCatalog.LEVEL_SLOTS * 3} STARS · GRAND FLOW" else if (round.stars == 3) "A flawless eruption" else "The portal awakens", style = TitleStyle, fontSize = 18.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Text("${round.moves} MOVES  ·  BEST ${profile.bestMovesFor(round.levelId,round.contentVersion) ?: round.moves}", color = Muted, fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("win_moves"))
            Text("YOUR REWARD", color = Gold, fontSize = 10.sp, letterSpacing = 2.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_coin_volcano), null, Modifier.size(48.dp))
                Text(" +${round.awardedCoins} COINS",
                    color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("win_reward"))
            }
            round.rewardBreakdown?.let { reward ->
                Text("LEVEL  +${reward.levelCoins}  ·  ${round.movesRemaining} MOVES LEFT  +${reward.efficiencyCoins}" +
                    if (reward.firstClearCoins > 0) "\nFIRST CLEAR  +${reward.firstClearCoins}" else "\nReplay reward · Every clear counts",
                    color = Gold, fontSize = 11.sp, lineHeight = 17.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("win_breakdown"))
            }
            GoldAction(if (round.levelId < GameCatalog.LEVEL_SLOTS) "NEXT LEVEL" else "LEVEL SELECT", {
                feedback.click()
                if (round.levelId < GameCatalog.LEVEL_SLOTS) model.openLevel(round.levelId + 1) {} else levels()
            }, Modifier.fillMaxWidth().height(60.dp), enabled = !state.busy, tag = "game_continue")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconAction("Replay", R.drawable.vr_rotate, "game_replay") { if (!state.busy) { feedback.click(); model.restartGame() } }
                IconAction("Levels", R.drawable.orn_compass, "game_levels", onClick = levels)
                IconAction("Home", R.drawable.ic_home_gold, "game_home", onClick = home)
            }
        }
        round.phase == RoundPhase.LOST -> GameDialog("OUT OF MOVES", home, "lose_dialog") {
            OutcomeCrest(false, 0, clock)
            Text("The flow has fallen silent", color = Muted, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("The portal is still sealed. Find a new path through the stone.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
            GoldAction("TRY AGAIN", { feedback.click(); model.restartGame() }, Modifier.fillMaxWidth(), enabled = !state.busy, tag = "game_retry")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconAction("Levels", R.drawable.orn_compass, "game_levels", onClick = levels)
                IconAction("Home", R.drawable.ic_home_gold, "game_home", onClick = home)
            }
        }
    }
}

@Composable
private fun InstructionStrip(message: String, action: String?, dismiss: () -> Unit) {
    RelicPanel(Modifier.fillMaxWidth().height(37.dp).padding(horizontal = 14.dp).testTag("game_instruction"), 8.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = Gold, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 2, modifier = Modifier.weight(1f))
            if (action != null) Box(Modifier.width(55.dp).fillMaxHeight().testTag("instruction_action").clickable(onClick = dismiss), contentAlignment = Alignment.Center) {
                Text(action, color = Cream, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable private fun animatedLavaClock(running: Boolean): Float {
    if (!running) return 0f
    val transition = rememberInfiniteTransition(label = "lavaMotion")
    val clock by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "lavaClock")
    return clock
}

@Composable
private fun BoardScene(round: PuzzleRound, graph: Connectivity, clock: Float, arrival: Float, running: Boolean,
                       ready: Boolean, selection: BoosterSelection?, teachRotation: Boolean, modifier: Modifier, turn: (Int) -> Unit) {
    val effectMotion = remember(round.runId) { Animatable(1f) }
    val flowGuides = remember(round.board,round.contentVersion) {
        round.board.tiles.indices.associateWith { FlowGuides.tile(round.definition,round.board,graph,it) }
    }
    val effect = round.effect
    LaunchedEffect(round.runId, round.revision, running) {
        if (effect != null && effect.booster != Booster.HINT && running) {
            effectMotion.snapTo(0f); effectMotion.animateTo(1f, tween(if (effect.booster == Booster.AUTO_ROTATE) 700 else 340, easing = FastOutSlowInEasing))
        } else effectMotion.snapTo(1f)
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // World objects join the frame at the fixed entry/exit; tile centers remain clear.
        val compactSix = round.board.columns == 6 && maxHeight < 480.dp
        val heightFactor = if (compactSix) 1.16f else 1.36f
        val side = minOf(maxWidth, maxHeight / heightFactor, 420.dp)
        val top = side * if (compactSix) .10f else .20f
        val inset = side * .077f
        val tileSide = (side - inset * 2) / round.board.columns
        val sourceX = inset + tileSide * (round.definition.source.adjacent.column + .5f)
        val goalX = inset + tileSide * (round.definition.goal.adjacent.column + .5f)
        val activated = round.phase == RoundPhase.WON || round.phase == RoundPhase.FLOWING && arrival > .9f
        Box(Modifier.width(side).height(side * heightFactor)) {
            Image(painterResource(R.drawable.vr_board_frame), null, Modifier.offset(y = top).size(side)
                .clip(RoundedCornerShape(side * .065f)), contentScale = ContentScale.FillBounds)
            Column(Modifier.offset(x = inset, y = top + inset).size(side - inset * 2).testTag("puzzle_board")) {
                repeat(round.board.rows) { row ->
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        repeat(round.board.columns) { column ->
                            val index = row * round.board.columns + column
                            val flowGuide = flowGuides.getValue(index)
                            val pathIndex = graph.route.indexOf(index)
                            val wave = if (round.phase == RoundPhase.FLOWING && pathIndex >= 0)
                                (arrival * (graph.route.size + 1) - pathIndex).coerceIn(0f, 1f) else if (round.phase == RoundPhase.WON) 1f else 0f
                            val swapOther = if (effect?.booster == Booster.SWAP && index in effect.indices)
                                effect.indices.first { it != index } else null
                            val valid = when (selection?.booster) {
                                Booster.HAMMER -> round.board.tiles[index].kind == TileKind.BLOCKED
                                Booster.SWAP -> round.board.tiles[index].kind.playable && (selection.first == null ||
                                    Direction.entries.any { round.board.neighbor(selection.first, it) == index })
                                else -> false
                            }
                            val hint = selection == null && (effect?.booster == Booster.HINT && index in effect.indices || teachRotation && index == 7)
                            val recentEffect = index in round.effect?.indices.orEmpty() && round.effect?.booster != Booster.HINT && effectMotion.value < 1f
                            ChannelTile(round.board.tiles[index], index, index in graph.reachable, graph.depths[index] ?: 0,
                                clock, wave, flowGuide, running, ready && effectMotion.value >= 1f && round.phase == RoundPhase.PLAYING && !round.paused,
                                round.board.columns, selection != null, hint || valid || recentEffect, selection?.first == index,
                                round.effect?.booster !in setOf(Booster.SWAP, Booster.HAMMER),
                                Modifier.weight(1f).fillMaxHeight().offset {
                                    val other = swapOther?.let(round.board::cell)
                                    IntOffset(if (other == null) 0 else ((other.column - column) * tileSide.toPx() * (1 - effectMotion.value)).toInt(),
                                        if (other == null) 0 else ((other.row - row) * tileSide.toPx() * (1 - effectMotion.value)).toInt())
                                }) { turn(index) }
                        }
                    }
                }
            }
            // Position ornaments from the authored endpoints, including six-column layouts.
            Canvas(Modifier.fillMaxSize()) {
                val tile = tileSide.toPx()
                moltenLine(Offset(sourceX.toPx(), (top + inset - 5.dp).toPx()), Offset(sourceX.toPx(), (top + inset + 1.dp).toPx()), tile * .204f, clock, StrokeCap.Butt)
                if (activated) {
                    val portalCenter = Offset(goalX.toPx(), (top + side + side * .025f).toPx())
                    drawCircle(Brush.radialGradient(listOf(Color(0xAAFF6C1A), Color(0x445F00A0), Color.Transparent), portalCenter, side.toPx() * .24f), side.toPx() * .24f, portalCenter)
                    moltenLine(Offset(goalX.toPx(), (top + side - inset).toPx()), Offset(goalX.toPx(), (top + side + side * .04f).toPx()), tile * .22f, clock)
                }
                if (effect?.booster == Booster.HAMMER && effectMotion.value < 1f) {
                    val target = round.board.cell(effect.indices.single())
                    val center = Offset((inset + tileSide * (target.column + .5f)).toPx(), (top + inset + tileSide * (target.row + .5f)).toPx())
                    val progress = effectMotion.value
                    repeat(9) { shard ->
                        val angle = shard * PI.toFloat() * 2 / 9
                        val distance = tile * progress * (.35f + shard % 3 * .12f)
                        drawCircle(if (shard % 2 == 0) Gold.copy(alpha = 1 - progress) else Color(0xFF9B527C).copy(alpha = 1 - progress),
                            tile * .045f * (1 - progress), center + Offset(cos(angle) * distance, sin(angle) * distance + progress * progress * tile * .3f))
                    }
                }
            }
            if (effect?.booster == Booster.HAMMER && effectMotion.value < 1f) {
                val target = round.board.cell(effect.indices.single())
                Image(painterResource(R.drawable.vr_hammer), null, Modifier.offset(x = inset + tileSide * target.column,
                    y = top + inset + tileSide * target.row - tileSide * .3f).size(tileSide).graphicsLayer {
                    rotationZ = -35 + effectMotion.value * 65; alpha = 1 - effectMotion.value
                })
            }
            RegisteredVolcano(tileSide * .264f, Modifier.align(Alignment.TopStart)
                .offset(x = sourceX - side * .19f).width(side * .38f).height(top + inset))
            Image(painterResource(R.drawable.vr_portal), "Destination portal", Modifier.align(Alignment.BottomStart)
                .offset(x = goalX - side * .145f).width(side * .29f).height(side * if (compactSix) .19f else .27f),
                alpha = if (activated) 1f else .88f, contentScale = ContentScale.Fit)
            if (activated) EmberField(Modifier.align(Alignment.BottomCenter).width(side * .38f).height(side * .27f), clock)
        }
    }
}

@Composable
private fun ChannelTile(tile: Tile, index: Int, reachable: Boolean, depth: Int, clock: Float, completionWave: Float, guide: FlowGuide,
                        running: Boolean, enabled: Boolean, columns: Int, selecting: Boolean, highlighted: Boolean, selected: Boolean,
                        animateRotation: Boolean, modifier: Modifier, tap: () -> Unit) {
    val angle by animateFloatAsState(tile.turns * 90f, tween(if (animateRotation) 230 else 0, easing = FastOutSlowInEasing), label = "quarterTurn$index")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .96f else 1f, tween(90), label = "tilePress")
    val fill by animateFloatAsState(if (reachable) 1f else 0f,
        tween(if (running) CHANNEL_TRAVEL_MS else 0,
            delayMillis = if (reachable && running) depth * CHANNEL_TRAVEL_MS else 0, easing = LinearEasing), label = "lavaReach$index")
    val dryId = when (tile.kind) {
        TileKind.STRAIGHT -> R.drawable.vr_straight_dry
        TileKind.CORNER -> R.drawable.vr_corner_dry
        TileKind.T_JUNCTION -> R.drawable.vr_t_dry
        TileKind.CROSS -> R.drawable.vr_cross_dry
        else -> R.drawable.vr_blocked
    }
    val wetId = when (tile.kind) {
        TileKind.STRAIGHT -> R.drawable.vr_straight_lava
        TileKind.CORNER -> R.drawable.vr_corner_lava
        TileKind.T_JUNCTION -> R.drawable.vr_t_lava
        TileKind.CROSS -> R.drawable.vr_cross_lava
        else -> R.drawable.vr_blocked
    }
    val dry = ImageBitmap.imageResource(dryId)
    val wet = ImageBitmap.imageResource(wetId)
    Box(modifier.testTag("tile_$index").graphicsLayer { scaleX = scale; scaleY = scale }.semantics(mergeDescendants = true) {
        contentDescription = "Row ${index / columns + 1} column ${index % columns + 1}, ${if (tile.kind == TileKind.CROSS) "fixed four-way cross" else tile.kind.name.lowercase().replace('_', ' ')}, ${tile.ports.joinToString { it.name.lowercase() }}"
        stateDescription = when { selected -> "Selected first tile"; highlighted -> "Highlighted useful tile"; reachable -> "Lava connected"; else -> "Dry" }
    }.clickable(enabled = enabled && (tile.kind.rotatable || selecting), indication = null, interactionSource = interaction,
        role = Role.Button, onClick = tap)) {
        Canvas(Modifier.fillMaxSize().padding(.6.dp).graphicsLayer { rotationZ = angle }.clip(RoundedCornerShape(3.dp))) {
            val target = IntSize(size.width.toInt(), size.height.toInt())
            drawImage(dry, dstSize = target, filterQuality = FilterQuality.High)
            if (fill > 0f && tile.kind.playable) {
                val localInlets = guide.inlets.map { (it.ordinal - tile.turns % 4 + 4) % 4 }.toSet()
                val branches = tile.kind == TileKind.T_JUNCTION || tile.kind == TileKind.CROSS
                // A junction receives the front at its inlet(s), then sends it down the outgoing arms.
                // It never ignites every arm from the center before the incoming stream arrives.
                val paths: List<Pair<Path,Float>> = if (branches) tile.kind.basePorts.map { port ->
                    val edge = Offset(size.width/2 + port.dc*size.width/2, size.height/2 + port.dr*size.height/2)
                    val incoming = port.ordinal in localInlets
                    Path().apply {
                        if (incoming) { moveTo(edge.x,edge.y); lineTo(size.width/2,size.height/2) }
                        else { moveTo(size.width/2,size.height/2); lineTo(edge.x,edge.y) }
                    } to (if (incoming) fill*2f else (fill-.5f)*2f).coerceIn(0f,1f)
                } else {
                    val reverse = if (tile.kind == TileKind.STRAIGHT) Direction.SOUTH.ordinal in localInlets
                        else Direction.EAST.ordinal in localInlets
                    val forward = channelShape(tile.kind,size.width,size.height,reverse)
                    if (localInlets.size < 2) listOf(forward to fill)
                    else listOf(forward to fill*.5f,channelShape(tile.kind,size.width,size.height,!reverse) to fill*.5f)
                }
                if (fill >= .999f) drawImage(wet, dstSize = target, filterQuality = FilterQuality.High)
                for ((path,progress) in paths) {
                    if (progress <= 0f) continue
                    val measure = PathMeasure().apply { setPath(path,false) }
                    val segment = Path()
                    measure.getSegment(0f,measure.length*progress,segment,true)
                    if (fill < .999f) {
                        val mask = android.graphics.Path()
                        android.graphics.Paint().apply {
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = size.width*.48f
                            strokeJoin = android.graphics.Paint.Join.ROUND
                        }.getFillPath(segment.asAndroidPath(),mask)
                        clipPath(mask.asComposePath()) { drawImage(wet,dstSize=target,filterQuality=FilterQuality.High) }
                    }
                    drawPath(segment,Color(0xFFFF8C1A).copy(alpha=.055f+completionWave*.065f),
                        style=Stroke(size.width*.24f,cap=StrokeCap.Butt,join=StrokeJoin.Round),blendMode=BlendMode.Plus)
                    // Quiet directional embers leave the illustrated lava and the channel width readable.
                    repeat(2) { spark ->
                        val f = (clock+spark*.5f+index*.071f)%1f*progress
                        val point = measure.getPosition(measure.length*f)
                        drawCircle(Color(0xFFFFD27A).copy(alpha=.42f),size.width*.009f,point,blendMode=BlendMode.Plus)
                    }
                }
            }
        }
        // Join only reciprocal live openings, after the visible front reaches that edge.
        // This spans the small image-border/gutter seam, never a rock or a mismatched neighbor.
        Canvas(Modifier.fillMaxSize()) {
            if (kotlin.math.abs(angle-tile.turns*90f) < .5f && fill > 0f) {
                for (port in guide.joined) {
                    val arrived = if (port in guide.inlets) fill > .06f else fill > .94f
                    if (!arrived) continue
                    val edge = Offset(size.width/2+port.dc*size.width/2,size.height/2+port.dr*size.height/2)
                    val inner = Offset(edge.x-port.dc*2.2.dp.toPx(),edge.y-port.dr*2.2.dp.toPx())
                    val outer = Offset(edge.x+port.dc*.7.dp.toPx(),edge.y+port.dr*.7.dp.toPx())
                    drawLine(Color(0xFFFF6E0D),inner,outer,size.width*.204f,StrokeCap.Butt)
                    drawLine(Color(0xFFFFBB39).copy(alpha=.7f),inner,outer,size.width*.105f,StrokeCap.Butt)
                }
            }
        }
        if (highlighted || selected) Canvas(Modifier.fillMaxSize().padding(2.dp)) {
            val pulse = .65f + .35f * sin(clock * PI.toFloat() * 2)
            drawRoundRect((if (selected) Color(0xFFE0A4FF) else Gold).copy(alpha = pulse),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()), style = Stroke(if (selected) 3.dp.toPx() else 2.dp.toPx()))
            drawRoundRect(Gold.copy(alpha = pulse * .13f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
        }
    }
}

/** Presentation masks only. Logical reciprocal ports decide connectivity. */
private fun channelShape(kind: TileKind, width: Float, height: Float, reverse: Boolean = false): Path = Path().apply {
    val cx = width / 2; val cy = height / 2
    when (kind) {
        TileKind.STRAIGHT -> { moveTo(cx, if (reverse) height else 0f); lineTo(cx, if (reverse) 0f else height) }
        TileKind.CORNER -> if (reverse) {
            moveTo(width, cy); lineTo(width * .76f, cy); cubicTo(width * .59f, cy, cx, height * .41f, cx, height * .24f); lineTo(cx, 0f)
        } else {
            moveTo(cx, 0f); lineTo(cx, height * .24f); cubicTo(cx, height * .41f, width * .59f, cy, width * .76f, cy); lineTo(width, cy)
        }
        TileKind.T_JUNCTION, TileKind.CROSS -> for (port in kind.basePorts) {
            moveTo(cx, cy); lineTo(cx + port.dc * width / 2, cy + port.dr * height / 2)
        }
        else -> Unit
    }
}

private fun DrawScope.moltenLine(start: Offset, end: Offset, width: Float, clock: Float, cap: StrokeCap = StrokeCap.Round) {
    drawLine(Color(0x33FF7000), start, end, width * 2.4f, cap)
    drawLine(Color(0xFFFF6300), start, end, width, cap)
    drawLine(Color(0xFFFFC744), start, end, width * .6f, cap)
    drawLine(Color(0xFFFFFFA2).copy(alpha = .7f + .15f * sin(clock * 2f * PI.toFloat())), start, end, width * .16f, cap)
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun GameDialog(title: String, dismiss: () -> Unit, tag: String, content: @Composable ColumnScope.() -> Unit) {
    Dialog(dismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().semantics { testTagsAsResourceId = true }, contentAlignment = Alignment.Center) {
            val compact = maxHeight < 700.dp
            val shortScreen = maxHeight < 600.dp
            if (tag == "win_dialog") EmberField(Modifier.fillMaxSize(), celebratory = true)
            ForgeFrame(Modifier.padding(horizontal = 17.dp).widthIn(max = 410.dp).heightIn(max = maxHeight - 12.dp)
                .verticalScroll(rememberScrollState()).testTag(tag), edge = 63.dp, subdued = tag == "lose_dialog") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = if (shortScreen) 20.dp else if (compact) 30.dp else 38.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (shortScreen) 4.dp else if (compact) 6.dp else 10.dp)) {
                    Text(if (shortScreen && title == "MASTER OF THE FLOW") "FLOW MASTER" else title,
                        style = TitleStyle, fontSize = if (shortScreen) 18.sp else if (compact) 22.sp else 24.sp,
                        letterSpacing = 1.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = if (shortScreen) 8.dp else 0.dp))
                    content()
                }
            }
        }
    }
}

