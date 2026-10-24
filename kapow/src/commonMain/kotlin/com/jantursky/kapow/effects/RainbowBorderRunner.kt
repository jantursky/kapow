package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import kotlin.time.Duration

/**
 * The stops of a seamless sweep gradient through [colors] and back to the first, turned by [phase] (0 to 1) of a
 * full turn. Turning the stops instead of the canvas keeps the outline itself still.
 */
internal fun turnedSweepStops(
    colors: List<Color>,
    phase: Float,
): Array<Pair<Float, Color>> {
    val n = colors.size
    if (n == 1) return arrayOf(0f to colors[0], 1f to colors[0])
    val turn = ((phase % 1f) + 1f) % 1f
    val stops = ArrayList<Pair<Float, Color>>(n + 2)
    for (i in 0 until n) stops += ((i.toFloat() / n + turn) % 1f) to colors[i]
    stops.sortBy { it.first }
    // The angle 0 falls between the last stop and the first one; both ends get the color there.
    val last = stops.last()
    val first = stops.first()
    val gap = 1f - last.first + first.first
    val seam = if (gap <= 0f) first.second else lerp(last.second, first.second, (1f - last.first) / gap)
    stops.add(0, 0f to seam)
    stops += 1f to seam
    return stops.toTypedArray()
}

/** An outline of [shape] stroked with a sweep of [colors] that turns once every [duration]. */
internal class RainbowBorderRunner(
    private val colors: List<Color>,
    private val width: Dp,
    shape: Shape,
    private val duration: Duration,
) : FadingLoopRunner() {
    private val outline = ShapeOutline(shape)

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val s = strength
        if (s <= 0f) return
        val stroke = width.toPx()
        outline.update(this, stroke / 2)
        val phase = if (isReduced) 0f else loopPhase(time, duration.inWholeMilliseconds / 1000.0)
        val brush = Brush.sweepGradient(*turnedSweepStops(colors, phase), center = center)
        drawPath(outline.path, brush, alpha = s, style = Stroke(stroke))
    }
}
