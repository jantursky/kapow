package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Dp
import com.jantursky.kapow.ShakeAxis
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration

/** Swings the element through its graphics layer, so nothing is laid out again. */
internal class ShakeRunner(
    private val axis: ShakeAxis,
    private val distance: Dp,
    private val swings: Int,
    private val rotation: Float,
    private val duration: Duration,
    private val decay: Float,
    private val easing: Easing = LinearEasing,
) : ChangeRunner() {
    private val progress = Animatable(1f)
    private val flash = Flash()
    private var job: Job? = null

    // The swing and rotation, as a share of their full size, where a restart began. They fade out over the new
    // shake, so it carries on from where the element was instead of jumping back to rest.
    private var carryOffset = 0f
    private var carryRotation = 0f

    override val needsLayer: Boolean get() = true

    private fun swing(): Float = shakeOffset(progress.value, swings, decay)

    private fun carried(): Float = 1f - progress.value

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val offset = (swing() + carryOffset * carried()) * distance.toPx()
        layer.translationX = if (axis == ShakeAxis.Vertical) 0f else offset
        layer.translationY = if (axis == ShakeAxis.Horizontal) 0f else offset
        layer.rotationZ = rotation * (swing() + carryRotation * carried())
        layer.compositingStrategy = flash.compositingStrategy
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        flash.draw(this)
    }

    override fun fire(context: EffectContext) {
        if (context.isReducedMotion()) {
            job?.cancel()
            if (context.showsReducedStandIn()) job = context.scope.launch { flash.play(context) }
            return
        }
        // Where the element is now, so the new shake starts from there.
        val running = progress.value < 1f
        carryOffset = if (running) swing() + carryOffset * carried() else 0f
        carryRotation = if (running) swing() + carryRotation * carried() else 0f
        job?.cancel()
        job =
            context.scope.launch {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(duration.at(context.speed).inWholeMilliseconds.toInt(), easing = easing))
                carryOffset = 0f
                carryRotation = 0f
            }
    }

    override fun stop() {
        job?.cancel()
    }
}
