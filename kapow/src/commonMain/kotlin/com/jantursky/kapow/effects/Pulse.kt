// Sending several pulses 0.2 s apart, with a matching cooldown, is derived from Pow
// (https://github.com/EmergeTools/Pow), MIT, see NOTICE.
package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.PulseStyle
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import com.jantursky.kapow.internal.awaitFrameTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The rectangle a pulse occupies at [progress]: the element's own bounds grown outward on every side by up to
 * [maxGrowth], fast at first and slowing down.
 */
internal fun pulseBounds(
    size: Size,
    progress: Float,
    maxGrowth: Float,
    startGrowth: Float = 0f,
): Rect {
    val eased = 1f - (1f - progress).let { it * it * it }
    val grow = startGrowth + (maxGrowth - startGrowth) * eased
    return Rect(0f - grow, 0f - grow, size.width + grow, size.height + grow)
}

/** How far a pulse grows: three quarters of the element's shorter side, at most 64 dp. */
internal fun pulseReach(
    size: Size,
    maxPx: Float,
): Float = min(min(size.width, size.height) * 0.75f, maxPx)

internal class PulseRunner(
    private val shape: Shape,
    private val color: Color,
    private val style: PulseStyle,
    private val count: Int,
    private val brush: Brush? = null,
    private val reach: Dp = Dp.Unspecified,
    private val duration: Duration? = null,
    private val interval: Duration = 200.milliseconds,
    private val easing: Easing = LinearEasing,
    private val startInset: Dp = 0.dp,
    private val fade: Easing? = null,
) : ChangeRunner() {
    private val pulses = mutableListOf<Animatable<Float, *>>()
    private val flash = Animatable(0f)
    private val jobs = mutableListOf<Job>()

    // Pulses live in a plain list; bumping this makes the draw phase notice a new one.
    private var generation by mutableIntStateOf(0)

    override fun ContentDrawScope.drawEffect() {
        if (generation < 0) return
        val f = flash.value
        if (f > 0f) drawShape(Rect(Offset.Zero, size), f * 0.3f, Fill)
        val grow = if (reach == Dp.Unspecified) pulseReach(size, 64.dp.toPx()) else reach.toPx()
        val startGrowth = -startInset.toPx()
        val stroke = (style as? PulseStyle.Stroke)?.let { Stroke(it.width.toPx()) }
        for (pulse in pulses) {
            val p = pulse.value
            if (p <= 0f || p >= 1f) continue
            val left = if (fade == null) 1f - p else 1f - fade.transform(p)
            val bounds = pulseBounds(size, p, grow, startGrowth)
            if (bounds.width <= 0f || bounds.height <= 0f) continue
            if (stroke != null) {
                drawShape(bounds, if (fade == null) left * left else left, stroke)
            } else {
                drawShape(bounds, left * 0.33f, Fill)
            }
        }
        drawContent()
    }

    private fun DrawScope.drawShape(
        bounds: Rect,
        alpha: Float,
        drawStyle: androidx.compose.ui.graphics.drawscope.DrawStyle,
    ) {
        val outline = shape.createOutline(bounds.size, layoutDirection, this)
        translate(bounds.left, bounds.top) {
            val brush = brush
            if (brush ==
                null
            ) {
                drawOutline(outline, color, alpha, drawStyle)
            } else {
                drawOutline(outline, brush, alpha, drawStyle)
            }
        }
    }

    override fun fire(context: EffectContext) {
        if (context.isReducedMotion()) {
            if (context.showsReducedStandIn()) {
                jobs +=
                    context.scope.launch {
                        flash.snapTo(1f)
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
                    }
            }
            return
        }
        val total = duration ?: if (style is PulseStyle.Fill) 2000.milliseconds else 1500.milliseconds
        val millis = total.at(context.speed).inWholeMilliseconds.toInt()
        val spacing = interval.at(context.speed)
        jobs.removeAll { !it.isActive }
        pulses.removeAll { !it.isRunning }
        generation++
        repeat(count) { index ->
            val pulse = Animatable(0f)
            pulses += pulse
            jobs +=
                context.scope.launch {
                    awaitFrameTime(spacing * index)
                    pulse.animateTo(1f, tween(millis, easing = easing))
                }
        }
    }

    override fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }
}
