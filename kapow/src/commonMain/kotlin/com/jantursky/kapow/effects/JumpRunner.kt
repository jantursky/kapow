package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Dp
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration

/** The widest the element gets when squashed, as a share of its width, at a squash of 1. */
private const val SQUASH_WIDEN = 0.15f

/** How much shorter the element gets when squashed, as a share of its height, at a squash of 1. */
private const val SQUASH_FLATTEN = 0.3f

/** Hops the element up through its graphics layer and squashes it on each landing. */
internal class JumpRunner(
    private val height: Dp,
    private val hops: Int,
    private val squash: Float,
    private val duration: Duration,
    private val easing: Easing = LinearEasing,
) : ChangeRunner() {
    private val progress = Animatable(1f)
    private val flash = Flash()
    private var job: Job? = null

    // How high the element was, in pixels, where a restart began. It fades out over the new jump.
    private var carry = 0f

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val t = progress.value
        val lift = jumpHeight(t, hops) * height.toPx() + carry * (1f - t)
        val squashed = jumpSquash(t, hops) * squash
        layer.translationY = -lift
        // Squashed against the ground, so the bottom edge stays put.
        layer.pivotOffset = Offset(size.width / 2f, size.height)
        layer.scaleX = 1f + SQUASH_WIDEN * squashed
        layer.scaleY = 1f - SQUASH_FLATTEN * squashed
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
        val running = progress.value < 1f
        val unit = context.density.run { height.toPx() }
        carry = if (running) jumpHeight(progress.value, hops) * unit + carry * (1f - progress.value) else 0f
        job?.cancel()
        job =
            context.scope.launch {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(duration.at(context.speed).inWholeMilliseconds.toInt(), easing = easing))
                carry = 0f
            }
    }

    override fun stop() {
        job?.cancel()
    }
}
