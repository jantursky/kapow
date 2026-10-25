package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Stretches a [dash] and [gap] pattern so a whole number of repeats fits around an outline [length] px long, and
 * the dashes meet without a seam where the outline closes. Returns the fitted dash and gap.
 */
internal fun fitDashes(
    length: Float,
    dash: Float,
    gap: Float,
): Pair<Float, Float> {
    val period = dash + gap
    if (length <= 0f || period <= 0f) return dash to gap
    val repeats = (length / period).roundToInt().coerceAtLeast(1)
    val stretch = length / (repeats * period)
    return dash * stretch to gap * stretch
}

/**
 * The dash and gap lengths to draw with round caps, which add half the stroke to each end of a dash: the dash is
 * shortened by [stroke] and the gap lengthened by it, so the pattern keeps its period. A dash shorter than the
 * stroke becomes a dot.
 */
internal fun roundCapPattern(
    dash: Float,
    gap: Float,
    stroke: Float,
): Pair<Float, Float> {
    val on = (dash - stroke).coerceAtLeast(0.01f)
    return on to (dash + gap - on)
}

/**
 * A dashed outline of [shape] whose dashes keep moving along it, over an optional [gapColor] line. They move
 * [clockwise] or not, start [dashOffset] along the pattern, end in round caps when [roundCaps] is set, and are
 * stroked with [brush] when set (instead of [color]).
 */
internal class MarchingAntsRunner(
    private val color: Color,
    private val gapColor: Color,
    private val width: Dp,
    shape: Shape,
    private val dash: Dp,
    private val gap: Dp,
    private val speed: Dp,
    private val brush: Brush? = null,
    private val clockwise: Boolean = true,
    private val roundCaps: Boolean = false,
    private val dashOffset: Dp = 0.dp,
) : FadingLoopRunner() {
    private val outline = ShapeOutline(shape)

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val s = strength
        if (s <= 0f) return
        val stroke = width.toPx()
        outline.update(this, stroke / 2)
        val (on, off) = fitDashes(outline.length, dash.toPx(), gap.toPx())
        val period = on + off
        val travelled = if (isReduced) 0.0 else time * speed.toPx()
        val wrapped = (travelled + dashOffset.toPx()) % period
        val phase = (if (wrapped < 0.0) wrapped + period else wrapped).toFloat()
        if (gapColor.alpha > 0f) drawPath(outline.path, gapColor, alpha = s, style = Stroke(stroke))
        // A falling phase moves the dashes forward along the outline, clockwise for the built-in shapes.
        val shift = if (clockwise) period - phase else phase
        val (visible, hidden) = if (roundCaps) roundCapPattern(on, off, stroke) else on to off
        val dashes = PathEffect.dashPathEffect(floatArrayOf(visible, hidden), shift)
        val style = Stroke(stroke, cap = if (roundCaps) StrokeCap.Round else StrokeCap.Butt, pathEffect = dashes)
        if (brush != null) {
            drawPath(outline.path, brush, alpha = s, style = style)
        } else {
            drawPath(outline.path, color, alpha = s, style = style)
        }
    }
}
