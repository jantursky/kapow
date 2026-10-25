package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * How far along an outline of [length] the comet's head is at [progress]. It runs past the end by the [tail]'s
 * length, so the tail follows the head back into the starting point and the comet leaves nothing behind.
 */
internal fun traceHead(
    progress: Float,
    length: Float,
    tail: Float,
): Float = progress.coerceIn(0f, 1f) * (length + tail)

/** The opacity of tail piece [index] of [count], counted from the head: full at the head, fading to nothing. */
internal fun tailAlpha(
    index: Int,
    count: Int,
): Float {
    val t = 1f - index.toFloat() / count
    return t * (0.5f + 0.5f * t)
}

/** A comet-like stroke that runs once around the element's [shape] outline, then disappears. */
internal class TraceRunner(
    shape: Shape,
    private val color: Color,
    private val width: Dp,
    private val duration: Duration,
    private val tailFraction: Float = TAIL,
    private val route: OutlineRoute = OutlineRoute(),
    private val easing: Easing = FastOutSlowInEasing,
    private val headGlow: Float = 1f,
    private val brush: Brush? = null,
) : ChangeRunner() {
    private val tracer = OutlineTracer(shape)
    private val core = if (brush == null) lerp(color, Color.White, 0.6f) else Color.White.copy(alpha = 0.6f)
    private val run = Animatable(0f)
    private val flash = Animatable(0f)
    private var job: Job? = null

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val f = flash.value
        val running = run.isRunning
        if (f <= 0f && !running) return
        val stroke = width.toPx()
        with(tracer) {
            measure(stroke)
            if (f > 0f) drawSpan(0f, length, color, stroke, f, brush = brush)
            if (running) drawComet(this@drawEffect, stroke, run.value)
        }
    }

    private fun OutlineTracer.drawComet(
        scope: ContentDrawScope,
        stroke: Float,
        progress: Float,
    ) = with(scope) {
        val tail = length * tailFraction
        val travelled = length * route.laps
        val head = traceHead(progress, travelled, tail)
        // A wide, faint pass under the front of the comet makes it read as light rather than a line.
        if (headGlow > 0f) {
            drawRoute(
                head - tail * 0.4f,
                head,
                route,
                color,
                stroke * 3f,
                (0.25f * headGlow).coerceAtMost(1f),
                brush = brush,
            )
        }
        for (i in 0 until PIECES) {
            val end = head - tail * i / PIECES
            val start = head - tail * (i + 1) / PIECES
            val cap = if (i == 0) StrokeCap.Round else StrokeCap.Butt
            drawRoute(start, end, route, color, stroke * (1f - 0.6f * i / PIECES), tailAlpha(i, PIECES), cap, brush)
        }
        drawRoute(head - tail / PIECES, head, route, core, stroke * 0.5f)
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        val millis = duration.at(context.speed).inWholeMilliseconds.toInt()
        job =
            context.scope.launch {
                if (context.isReducedMotion()) {
                    if (!context.showsReducedStandIn()) return@launch
                    // The whole outline shows at once and fades: nothing travels.
                    flash.snapTo(0.8f)
                    flash.animateTo(
                        0f,
                        tween(
                            400
                                .milliseconds
                                .at(context.speed)
                                .inWholeMilliseconds
                                .toInt(),
                        ),
                    )
                } else {
                    run.snapTo(0f)
                    run.animateTo(1f, tween(millis, easing = easing))
                    run.snapTo(0f)
                }
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        // The tail is this share of the outline, drawn in this many pieces that thin out and fade.
        const val TAIL = 0.4f
        const val PIECES = 12
    }
}
