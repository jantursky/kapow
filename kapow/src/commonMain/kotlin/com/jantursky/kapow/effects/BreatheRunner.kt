package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Dp
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos

/**
 * How far into a breath the element is at [time] seconds, for breaths of [period] seconds: 0 at rest, 1 at the
 * fullest, easing in and out of both.
 */
internal fun breatheWave(
    time: Double,
    period: Double,
): Float = if (period <= 0.0) 0f else (0.5 - 0.5 * cos(2.0 * PI * time / period)).toFloat()

/** The scale at [wave] (0 to 1) of a breath that reaches [peak], when the effect is [amount] (0 to 1) of the way in. */
internal fun breatheScale(
    wave: Float,
    amount: Float,
    peak: Float,
): Float = 1f + (peak - 1f) * wave * amount

/**
 * Swells the element slowly to [scale] and back, and lifts it by [float] at the fullest, on a loop while active.
 * The effect itself eases in and out on a spring, so the element settles back to rest without a jump. With reduced
 * motion nothing moves.
 */
internal class BreatheRunner(
    private val scale: Float,
    private val float: Dp,
    private val period: Double,
) : ConditionalRunner() {
    private val amount = Animatable(0f)
    private val clock = LoopClock()
    private var job: Job? = null

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val k = amount.value
        if (k <= 0f) {
            layer.scaleX = 1f
            layer.scaleY = 1f
            layer.translationY = 0f
            return
        }
        val wave = breatheWave(clock.seconds, period)
        val s = breatheScale(wave, k, scale)
        layer.scaleX = s
        layer.scaleY = s
        layer.translationY = -float.toPx() * wave * k
    }

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        job?.cancel()
        if (context.isReducedMotion()) {
            if (!active) clock.stop()
            return
        }
        if (active) clock.start(context.scope, context.speed)
        val target = if (active) 1f else 0f
        val chosen = if (active) context.fadeIn else context.fadeOut
        val speed = context.speed
        job =
            context.scope.launch {
                if (chosen != null) {
                    amount.animateTo(target, tween(chosen.inWholeMilliseconds.toInt()))
                } else {
                    amount.animateTo(target, spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow * speed * speed))
                }
                if (!active) clock.stop()
            }
    }

    override fun stop() {
        job?.cancel()
        clock.stop()
    }
}
