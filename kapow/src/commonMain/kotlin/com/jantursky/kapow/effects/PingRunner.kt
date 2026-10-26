package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import com.jantursky.kapow.ReducedMotionMode
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.floor

/**
 * How far ring [index] of [rings] is through its life at [time] seconds, from 0 (just born) to 1 (gone), or null
 * while it is not alive. Rings last [duration] seconds and are born evenly, one every `duration / rings`, from
 * time 0 on, the first ring first.
 *
 * A ring is only born when the effect is on: not after [offAt] seconds and before [onAt] seconds, so rings in
 * flight when the condition turns false finish and no new one starts.
 */
internal fun pingPhase(
    time: Double,
    duration: Double,
    rings: Int,
    index: Int,
    offAt: Double = Double.POSITIVE_INFINITY,
    onAt: Double = Double.POSITIVE_INFINITY,
): Float? {
    if (duration <= 0.0 || rings <= 0) return null
    val first = index * duration / rings
    if (time < first) return null
    val born = first + floor((time - first) / duration) * duration
    if (born > offAt && born < onAt) return null
    return ((time - born) / duration).toFloat().coerceIn(0f, 1f)
}

/** How much a ring has grown at [progress] of its life, from 1 to [scale]: fast at first, then slowing down. */
internal fun pingScale(
    progress: Float,
    scale: Float,
): Float {
    val left = 1f - progress.coerceIn(0f, 1f)
    return 1f + (scale - 1f) * (1f - left * left * left)
}

/** How visible a ring is at [progress] of its life: fully at birth, fading to nothing. */
internal fun pingAlpha(progress: Float): Float {
    val left = 1f - progress.coerceIn(0f, 1f)
    return left * left
}

/**
 * Rings in the element's [shape] that keep growing out of its outline and fading while active, one born every
 * `duration / rings`. When the condition ends no new ring starts and the ones in flight finish, unless
 * `fadeOut(...)` is set, which fades them out instead. With reduced motion one faint ring stays still.
 */
internal class PingRunner(
    private val color: Color,
    private val shape: Shape,
    private val rings: Int,
    private val scale: Float,
    private val duration: Double,
    private val width: Dp,
) : ConditionalRunner() {
    private val clock = LoopClock()
    private val level = Animatable(0f)
    private var job: Job? = null
    private var offAt = Double.POSITIVE_INFINITY
    private var onAt = Double.POSITIVE_INFINITY
    private var reduced by mutableStateOf(false)
    private var hidden = false

    override fun ContentDrawScope.drawEffect() {
        val k = level.value
        if (k > 0f && !hidden) {
            val stroke = Stroke(width.toPx())
            if (reduced) {
                drawRing(REDUCED_PHASE, k * REDUCED_ALPHA, stroke)
            } else {
                val time = clock.seconds
                for (i in 0 until rings) {
                    val phase = pingPhase(time, duration, rings, i, offAt, onAt) ?: continue
                    drawRing(phase, k * pingAlpha(phase), stroke)
                }
            }
        }
        drawContent()
    }

    private fun ContentDrawScope.drawRing(
        phase: Float,
        alpha: Float,
        stroke: Stroke,
    ) {
        if (alpha <= 0f) return
        val s = pingScale(phase, scale)
        val ringSize = Size(size.width * s, size.height * s)
        val outline = shape.createOutline(ringSize, layoutDirection, this)
        translate((size.width - ringSize.width) / 2f, (size.height - ringSize.height) / 2f) {
            drawOutline(outline, color, alpha, stroke)
        }
    }

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        job?.cancel()
        if (active) {
            reduced = context.isReducedMotion()
            hidden = reduced && context.reducedMotionMode == ReducedMotionMode.EndState
        }
        if (reduced) {
            val fades = context.reducedMotionMode == ReducedMotionMode.Fade
            job =
                context.scope.launch {
                    val target = if (active) 1f else 0f
                    if (fades) level.animateTo(target, tween(REDUCED_FADE_MILLIS)) else level.snapTo(target)
                }
            return
        }
        if (active) {
            if (clock.isRunning) {
                onAt = clock.seconds
            } else {
                clock.reset()
                offAt = Double.POSITIVE_INFINITY
                onAt = Double.POSITIVE_INFINITY
            }
            clock.start(context.scope, context.speed) { finishIfIdle() }
            val fadeIn = context.fadeIn
            job =
                context.scope.launch {
                    if (fadeIn !=
                        null
                    ) {
                        level.animateTo(1f, tween(fadeIn.inWholeMilliseconds.toInt()))
                    } else {
                        level.snapTo(1f)
                    }
                }
        } else {
            offAt = clock.seconds
            onAt = Double.POSITIVE_INFINITY
            val fadeOut = context.fadeOut
            if (fadeOut != null) {
                job =
                    context.scope.launch {
                        level.animateTo(0f, tween(fadeOut.inWholeMilliseconds.toInt()))
                        clock.stop()
                    }
            }
        }
    }

    // Once the last ring has finished there is nothing left to draw, so the clock stops.
    private fun finishIfIdle() {
        if (offAt.isFinite() && onAt.isInfinite() && clock.seconds - offAt >= duration) {
            clock.stop()
        }
    }

    override fun stop() {
        job?.cancel()
        clock.stop()
    }

    private companion object {
        const val REDUCED_PHASE = 0.4f
        const val REDUCED_ALPHA = 0.3f
        const val REDUCED_FADE_MILLIS = 150
    }
}
