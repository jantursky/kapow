package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp
import com.jantursky.kapow.internal.ProgressRunner

/** Draws the element's [shape] outline as a stroke from its start up to the progress. */
internal class TraceProgressRunner(
    shape: Shape,
    private val color: Color,
    private val width: Dp,
) : ProgressRunner() {
    private val tracer = OutlineTracer(shape)

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val p = progress().coerceIn(0f, 1f)
        if (p <= 0f) return
        val stroke = width.toPx()
        with(tracer) {
            measure(stroke)
            drawSpan(0f, length * p, color, stroke)
        }
    }
}
