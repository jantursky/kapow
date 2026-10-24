package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
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

/** A dashed outline of [shape] whose dashes keep moving along it, over an optional [gapColor] line. */
internal class MarchingAntsRunner(
    private val color: Color,
    private val gapColor: Color,
    private val width: Dp,
    shape: Shape,
    private val dash: Dp,
    private val gap: Dp,
    private val speed: Dp,
) : FadingLoopRunner() {
    private val outline = ShapeOutline(shape)

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val s = strength
        if (s <= 0f) return
        val stroke = width.toPx()
        outline.update(this, stroke / 2)
        val (on, off) = fitDashes(outline.length, dash.toPx(), gap.toPx())
        val phase = if (isReduced) 0f else ((time * speed.toPx()) % (on + off)).toFloat()
        if (gapColor.alpha > 0f) drawPath(outline.path, gapColor, alpha = s, style = Stroke(stroke))
        // A falling phase moves the dashes forward along the outline, clockwise for the built-in shapes.
        val dashes = PathEffect.dashPathEffect(floatArrayOf(on, off), on + off - phase)
        drawPath(outline.path, color, alpha = s, style = Stroke(stroke, pathEffect = dashes))
    }
}
