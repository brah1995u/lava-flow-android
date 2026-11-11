package com.lavaflow.puzzle.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt

internal val LaunchNight = Color(0xFF170D26)

/** A short cold-start introduction, concurrent with real save loading. No fake load percentage.
 *  Foreground time only; returning Home or recreating the activity does not replay it. */
@Composable
internal fun rememberLaunchIntroFinished(): Boolean {
    var finished by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (finished) 1f else 0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var active by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            active = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(active, finished) {
        if (active && !finished) {
            progress.animateTo(1f, tween(((1f - progress.value) * 900).roundToInt(), easing = LinearEasing))
            finished = true
        }
    }
    return finished
}

/**
 * Restores the earlier strong Home hierarchy from the existing transparent title and volcano art.
 * The two bitmaps are intentionally layered instead of being baked into a new placeholder scene.
 */
@Composable
internal fun ClassicVolcanicHero(
    titleArtwork: Painter,
    volcanoArtwork: Painter,
    compact: Boolean,
    modifier: Modifier = Modifier,
    tag: String,
) {
    BoxWithConstraints(modifier.testTag(tag), contentAlignment = Alignment.TopCenter) {
        val titleWidth = minOf(maxWidth * if (compact) .68f else .88f, if (compact) 200.dp else 330.dp)
        val volcanoWidth = minOf(maxWidth * if (compact) .25f else .38f, if (compact) 70.dp else 138.dp)
        Image(titleArtwork, "Lava Flow", Modifier.width(titleWidth).align(Alignment.TopCenter), contentScale = ContentScale.Fit)
        Image(volcanoArtwork, "Volcano", Modifier.width(volcanoWidth).align(Alignment.BottomCenter)
            .offset(y = if (compact) 8.dp else 10.dp), contentScale = ContentScale.Fit)
    }
}

@Composable
internal fun StartupPresentation(
    visible: Boolean,
    ready: Boolean,
    error: String?,
    retry: () -> Unit,
    titleArtwork: Painter,
    volcanoArtwork: Painter,
    clock: Float,
) {
    AnimatedVisibility(visible, exit = fadeOut(tween(240))) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Transparent).testTag("startup")
            .semantics { paneTitle = "Lava Flow startup" }
            .pointerInput(Unit) {
                // The foreground surface catches background taps; RETRY consumes its own gestures.
                detectTapGestures { }
            }, contentAlignment = Alignment.Center) {
            val compact = maxHeight < 650.dp
            EmberField(Modifier.fillMaxSize(), clock)
            Column(Modifier.fillMaxWidth().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                ClassicVolcanicHero(titleArtwork, volcanoArtwork, compact,
                    Modifier.fillMaxWidth().height(if (compact) 180.dp else 320.dp), tag = "startup_hero")
                Text("ROTATE  ·  CONNECT  ·  IGNITE", color = Gold.copy(alpha = .85f), fontSize = 11.sp,
                    letterSpacing = 1.4.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
                Spacer(Modifier.height(36.dp))
                if (error == null) {
                    LavaLoadingBand(clock, ready)
                    Text(if (ready) "Find your flow" else "Loading your progress…", color = Cream,
                        fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 15.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite })
                } else {
                    Text(error, color = Cream, fontSize = 14.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp).semantics { liveRegion = LiveRegionMode.Polite })
                    GoldAction("RETRY", retry, Modifier.padding(top = 18.dp).widthIn(max = 280.dp).fillMaxWidth(), tag = "startup_retry")
                }
            }
        }
    }
}

@Composable
private fun LavaLoadingBand(clock: Float, ready: Boolean) {
    val shape = RoundedCornerShape(50)
    Box(Modifier.width(232.dp).height(14.dp).clip(shape).background(Purple)
        .border(1.dp, Gold.copy(alpha = .7f), shape)
        .semantics {
            contentDescription = if (ready) "Opening Lava Flow" else "Loading saved progress"
            if (!ready) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }) {
        Canvas(Modifier.fillMaxSize().padding(3.dp).clip(shape)) {
            drawRect(Color(0xFF391425))
            val band = size.width * .5f
            val x = ((clock * 4f) % 1f) * (size.width + band) - band
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFFF790C),
                Color(0xFFFFDF80), Color(0xFFFF790C), Color.Transparent), x, x + band),
                topLeft = Offset(x, 0f), size = Size(band, size.height))
        }
    }
}
