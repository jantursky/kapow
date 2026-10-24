package com.jantursky.kapow.samples.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.EffectCard
import com.jantursky.kapow.samples.design.Stage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The shape of every example page: the live stage on top, its controls under it. While the host's autoplay
 * ([GalleryStage]) is on, [autoplay] plays the example over and over. Inside an [EffectTile] only the stage
 * shows, scaled to the tile, and the controls stay out.
 */
@Composable
internal fun ExampleScaffold(
    autoplay: Autoplay,
    stage: @Composable () -> Unit,
    controls: @Composable ColumnScope.() -> Unit,
) {
    val onTouch = runAutoplay(autoplay, LocalGalleryAutoplay.current)
    if (LocalExamplePreview.current) {
        PreviewStage(stage)
        return
    }
    EffectCard(Modifier.fillMaxSize().onTouch(onTouch)) {
        Stage(Modifier.weight(1f)) { stage() }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = controls)
    }
}

/** How long autoplay waits when the page opens or Play is pressed, so the first step is seen from the start. */
private val LEAD = 600.milliseconds

/**
 * Runs [autoplay] while [playing]: a step, then a wait on the page's frame clock, again and again. Pausing stops
 * the loop but lets a running step finish. Returns what to call when the visitor touches the example: that
 * cancels a running step and restarts the wait, so autoplay never fights the visitor's own controls.
 */
@Composable
private fun runAutoplay(
    autoplay: Autoplay,
    playing: Boolean,
): () -> Unit {
    val current by rememberUpdatedState(autoplay)
    // Steps run in the page's scope rather than the loop's, so they outlive a pause.
    val scope = rememberCoroutineScope()
    val running = remember { mutableStateOf<Job?>(null) }
    val touched = remember { mutableStateOf(false) }
    val touches = remember { mutableIntStateOf(0) }
    LaunchedEffect(playing, touches.intValue) {
        if (!playing) {
            // Play after a pause starts soon, whatever was touched in between.
            touched.value = false
            return@LaunchedEffect
        }
        var wait = if (touched.value || current.startsOn) current.every() else LEAD
        touched.value = false
        while (true) {
            waitFrames(wait)
            val step = scope.launch { current.step() }
            running.value = step
            step.join()
            wait = current.every()
        }
    }
    return remember {
        {
            running.value?.cancel()
            touched.value = true
            touches.intValue++
        }
    }
}

/** Waits [duration] on the frame clock, so autoplay keeps the page's pace and rests while the tab is hidden. */
private suspend fun waitFrames(duration: Duration) {
    val start = withFrameNanos { it }
    while (withFrameNanos { it } - start < duration.inWholeNanoseconds) continue
}

/** Calls [onTouch] when a pointer presses or scrolls inside, or a key goes down, without taking the event. */
private fun Modifier.onTouch(onTouch: () -> Unit): Modifier =
    pointerInput(onTouch) {
        awaitPointerEventScope {
            while (true) {
                val type = awaitPointerEvent(PointerEventPass.Initial).type
                if (type == PointerEventType.Press || type == PointerEventType.Scroll) onTouch()
            }
        }
    }.onPreviewKeyEvent {
        if (it.type == KeyEventType.KeyDown) onTouch()
        false
    }
