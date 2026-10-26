package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer

/**
 * How far a shimmer is through its sweep at [time] seconds, from 0 (the band is just outside one end) to 1 (it has
 * just left the other), for sweeps of [sweep] seconds with [pause] seconds of nothing between them. Null during a
 * pause.
 */
internal fun shimmerProgress(
    time: Double,
    sweep: Double,
    pause: Double,
): Float? {
    if (sweep <= 0.0) return null
    val period = sweep + pause.coerceAtLeast(0.0)
    val at = ((time % period) + period) % period
    return if (at < sweep) (at / sweep).toFloat() else null
}

/** Draws the shimmer band at [progress], or an even wash when it is still, over the pixels already drawn. */
internal fun DrawScope.drawShimmer(
    stops: Array<Pair<Float, Color>>,
    angle: Float,
    progress: Float?,
    alpha: Float,
    bandFraction: Float,
    blendMode: BlendMode,
) {
    if (alpha <= 0f) return
    if (progress == null) return
    drawShine(stops, angle, progress, alpha, bandFraction, blendMode = blendMode)
}

/**
 * A band of light that keeps sweeping the element's pixels, with a [pause] between sweeps, while it is on. Under
 * reduced motion the element is lightened evenly and nothing travels.
 */
internal class ShimmerRunner(
    color: Color,
    private val angle: Float,
    private val sweep: Double,
    private val pause: Double,
    private val bandFraction: Float,
    softness: Float,
    private val blendMode: BlendMode,
) : FadingLoopRunner(fadeMillis = FADE_MILLIS) {
    private val stops = shineStops(color, softness, peak = 1f)
    private val wash = color.copy(alpha = color.alpha * WASH_SHARE)

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy =
            if (strength > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val s = strength
        if (s <= 0f) return
        if (isReduced) {
            drawRect(wash, alpha = s, blendMode = blendMode)
        } else {
            drawShimmer(stops, angle, shimmerProgress(time, sweep, pause), s, bandFraction, blendMode)
        }
    }

    private companion object {
        const val FADE_MILLIS = 200
        const val WASH_SHARE = 0.5f
    }
}
