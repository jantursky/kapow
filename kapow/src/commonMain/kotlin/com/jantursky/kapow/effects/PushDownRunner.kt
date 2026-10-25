package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ReducedMotionMode
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Scales the element down on a spring while active, about [origin], moving it down by [translation] and dimming
 * it by [dim] on the way. With reduced motion it dims (by [dim] or, without one, a fifth) instead of moving.
 *
 * The spring is stiffer by the square of the effect's speed. A fade set with `fadeIn(...)` or `fadeOut(...)`
 * replaces it with a plain animation of that length.
 */
internal class PushDownRunner(
    private val scale: Float,
    private val damping: Float = DAMPING,
    private val stiffness: Float = STIFFNESS,
    private val dim: Float = 0f,
    private val translation: Dp = 0.dp,
    private val origin: TransformOrigin = TransformOrigin.Center,
) : ConditionalRunner() {
    private val press = Animatable(0f)
    private var reduced = false
    private var job: Job? = null

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val p = press.value
        val s = if (reduced) 1f else 1f + (scale - 1f) * p
        layer.scaleX = s
        layer.scaleY = s
        layer.pivotOffset =
            if (origin == TransformOrigin.Center) {
                Offset.Unspecified
            } else {
                Offset(size.width * origin.pivotFractionX, size.height * origin.pivotFractionY)
            }
        layer.translationY = if (reduced) 0f else translation.toPx() * p
        val dimmed = if (reduced) maxOf(dim, DIM) else dim
        layer.alpha = 1f - dimmed * p
    }

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        job?.cancel()
        val target = if (active) 1f else 0f
        val chosen = if (active) context.fadeIn else context.fadeOut
        val speed = context.speed
        job =
            context.scope.launch {
                if (active) reduced = context.isReducedMotion()
                if (reduced && context.reducedMotionMode == ReducedMotionMode.Fade) {
                    press.animateTo(target, tween(150))
                } else if (reduced) {
                    press.snapTo(target)
                } else if (chosen != null) {
                    press.animateTo(target, tween(chosen.inWholeMilliseconds.toInt()))
                } else {
                    press.animateTo(target, spring(dampingRatio = damping, stiffness = stiffness * speed * speed))
                }
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        const val DIM = 0.2f
        const val DAMPING = 0.6f
        const val STIFFNESS = 500f
    }
}
