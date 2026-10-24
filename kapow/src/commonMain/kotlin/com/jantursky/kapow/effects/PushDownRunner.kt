package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.GraphicsLayerScope
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Scales the element down on a spring while active. With reduced motion it dims instead of shrinking. */
internal class PushDownRunner(
    private val scale: Float,
) : ConditionalRunner() {
    private val press = Animatable(0f)
    private var reduced = false
    private var job: Job? = null

    override val needsLayer: Boolean get() = true

    override fun GraphicsLayerScope.applyLayer() {
        val p = press.value
        if (reduced) {
            alpha = 1f - DIM * p
        } else {
            val s = 1f + (scale - 1f) * p
            scaleX = s
            scaleY = s
        }
    }

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        job?.cancel()
        val target = if (active) 1f else 0f
        job =
            context.scope.launch {
                if (active) reduced = context.isReducedMotion()
                if (reduced) {
                    press.snapTo(target)
                } else {
                    press.animateTo(target, spring(dampingRatio = 0.6f, stiffness = 500f))
                }
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        const val DIM = 0.2f
    }
}
