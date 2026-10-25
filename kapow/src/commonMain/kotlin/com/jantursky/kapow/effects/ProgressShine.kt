package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.jantursky.kapow.internal.ProgressRunner
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Where the gradient of band [index] of [count] starts and ends at [progress], for bands that are [bandFraction]
 * of the element's length across, [spacing] of the length apart, sweeping in the direction of [angle] degrees. The
 * bands start fully outside the element and end fully outside it on the other side.
 */
internal fun progressShineBand(
    size: Size,
    angle: Float,
    progress: Float,
    bandFraction: Float,
    spacing: Float,
    index: Int,
    count: Int,
): Pair<Offset, Offset> {
    val radians = angle * PI.toFloat() / 180f
    val direction = Offset(cos(radians), sin(radians))
    // How long the element is when measured along the sweep direction.
    val length = abs(size.width * direction.x) + abs(size.height * direction.y)
    val band = length * bandFraction
    val reach = (count - 1) / 2f * spacing * length
    val travel = length / 2 + band / 2 + reach
    val shift = (index - (count - 1) / 2f) * spacing * length
    val center = Offset(size.width / 2, size.height / 2) + direction * (-travel + 2 * travel * progress + shift)
    return (center - direction * (band / 2)) to (center + direction * (band / 2))
}

/**
 * The band's colors: transparent at both edges, strongest in the middle. [softness] 1 is the usual falloff; more
 * spreads the glow wider, less pulls it into a tighter core.
 */
internal fun progressShineStops(
    color: Color,
    softness: Float,
): Array<Pair<Float, Color>> {
    if (softness == 1f) return shineStops(color)
    return Array(STEPS + 1) { i ->
        val x = i.toFloat() / STEPS
        val falloff = sin(x * PI.toFloat()).pow(2f / softness)
        x to color.copy(alpha = color.alpha * falloff * 0.8f)
    }
}

private const val STEPS = 10

/** Bands of light whose position across the element follows the progress. */
internal class ShineProgressRunner(
    private val angle: Float,
    color: Color,
    private val bandWidth: Float = 0.6f,
    softness: Float = 1f,
    private val bands: Int = 1,
    private val bandSpacing: Float = 0.3f,
    private val blendMode: BlendMode = BlendMode.SrcAtop,
) : ProgressRunner() {
    private val stops = progressShineStops(color, softness)

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy = CompositingStrategy.Offscreen
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val t = progress().coerceIn(0f, 1f)
        if (t <= 0f || t >= 1f) return
        for (i in 0 until bands) {
            val (start, end) = progressShineBand(size, angle, t, bandWidth, bandSpacing, i, bands)
            drawRect(Brush.linearGradient(*stops, start = start, end = end), blendMode = blendMode)
        }
    }
}
