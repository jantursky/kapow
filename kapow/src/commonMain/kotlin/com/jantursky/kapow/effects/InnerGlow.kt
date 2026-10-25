package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A glow just inside the edge of an element's [shape]: brightest at the edge, fading towards the middle. It
 * stacks a few strokes of growing width, clipped to the shape, so it needs no blur and looks the same everywhere.
 */
internal class InnerGlow(
    private val color: Color,
    private val radius: Dp,
    private val shape: Shape,
    private val strength: Float,
    private val limit: Float = MAX_HALO,
) {
    // The clip only changes with the element's size, so it is built once per size.
    private val path = Path()
    private var pathSize = Size.Unspecified
    private var pathDirection: LayoutDirection? = null

    fun draw(
        scope: DrawScope,
        amount: Float,
    ) = with(scope) {
        val a = amount.coerceIn(0f, limit)
        if (a <= 0f) return@with
        if (size != pathSize || layoutDirection != pathDirection) {
            path.reset()
            path.addOutline(shape.createOutline(size, layoutDirection, this))
            pathSize = size
            pathDirection = layoutDirection
        }
        val reach = radius.toPx() * a
        val alpha = (haloOpacity(a) * strength).coerceIn(0f, 1f) * STEP_ALPHA
        clipPath(path) {
            for (step in 1..STEPS) {
                // A stroke is centered on the edge, so only half its width lies inside.
                drawPath(path, color, alpha, Stroke(2f * reach * step / STEPS))
            }
        }
    }

    private companion object {
        const val STEPS = 6
        const val STEP_ALPHA = 0.3f
    }
}
