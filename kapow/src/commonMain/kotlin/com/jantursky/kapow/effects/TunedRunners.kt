package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.ReducedMotionMode
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.ProgressRunner
import com.jantursky.kapow.internal.TunedContext
import com.jantursky.kapow.internal.awaitFrameTime
import com.jantursky.kapow.internal.skipsForReducedMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Runs [inner] with the fade, delay, speed and reduced-motion settings of a `ConditionalEffect.Tuned` wrapper. */
internal class TunedConditionalRunner(
    private val inner: ConditionalRunner,
    private val tuned: ConditionalEffect.Tuned,
) : ConditionalRunner() {
    private var pending: Job? = null

    override val needsLayer: Boolean get() = inner.needsLayer

    override fun DrawScope.configureLayer(layer: GraphicsLayer) = with(inner) { configureLayer(layer) }

    override fun ContentDrawScope.drawEffect() = with(inner) { drawEffect() }

    override fun attach(context: EffectContext) = inner.attach(wrap(context))

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        pending?.cancel()
        val wrapped = wrap(context)
        if (!active) {
            inner.setActive(wrapped, false)
        } else if (wrapped.skipsForReducedMotion()) {
            return
        } else if (tuned.startDelay.isPositive()) {
            pending =
                context.scope.launch {
                    awaitFrameTime(tuned.startDelay)
                    inner.setActive(wrapped, true)
                }
        } else {
            inner.setActive(wrapped, true)
        }
    }

    override fun stop() {
        pending?.cancel()
        inner.stop()
    }

    private fun wrap(context: EffectContext): EffectContext =
        TunedContext(
            context,
            tuned.speed,
            tuned.mode ?: ReducedMotionMode.Default,
            tuned.fadeIn,
            tuned.fadeOut,
        )
}

/**
 * Feeds [inner] the progress after [curve] and then [smoothing] (a spring following it), and draws nothing under
 * reduced motion when [mode] is `Nothing`. Progress effects honor no other mode: they are always drawn at the
 * progress you pass.
 */
internal class ShapedProgressRunner(
    private val inner: ProgressRunner,
    private val curve: Easing?,
    private val smoothing: Pair<Float, Float>?,
    private val mode: ReducedMotionMode,
) : ProgressRunner() {
    private var smoothed by mutableFloatStateOf(0f)
    private var hidden = false

    init {
        inner.progress = {
            if (smoothing != null) smoothed else shaped(progress())
        }
    }

    private fun shaped(value: Float): Float {
        val clamped = value.coerceIn(0f, 1f)
        return curve?.transform(clamped) ?: clamped
    }

    override val needsLayer: Boolean get() = inner.needsLayer

    override fun DrawScope.configureLayer(layer: GraphicsLayer) = with(inner) { configureLayer(layer) }

    override fun ContentDrawScope.drawEffect() {
        if (hidden) drawContent() else with(inner) { drawEffect() }
    }

    override fun attach(context: EffectContext) {
        hidden = mode == ReducedMotionMode.Nothing && context.isReducedMotion()
        inner.attach(context)
        if (smoothing == null) return
        val (damping, stiffness) = smoothing
        context.scope.launch {
            var animatable: Animatable<Float, *>? = null
            snapshotFlow { shaped(progress()) }.collectLatest { target ->
                val current = animatable
                if (current == null) {
                    smoothed = target
                    animatable = Animatable(target)
                } else {
                    current.animateTo(target, spring(damping, stiffness)) { smoothed = value }
                }
            }
        }
    }

    override fun stop() = inner.stop()
}
