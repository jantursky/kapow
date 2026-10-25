package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.jantursky.kapow.internal.ProgressRunner

/**
 * The stretch of an outline [length] px long that a trace [progress] of the way round covers, as its start and
 * end distances along the outline (either may be outside 0 to [length]; wrap them with [wrappedSpans]). The trace
 * starts [start] of the way round the outline, goes [clockwise] or not and covers at most the last [tail] of the
 * outline behind its head.
 */
internal fun traceStretch(
    length: Float,
    progress: Float,
    start: Float,
    clockwise: Boolean,
    tail: Float,
): Pair<Float, Float> {
    val at = start * length
    val travelled = length * progress.coerceIn(0f, 1f)
    val trailing = (travelled - tail * length).coerceAtLeast(0f)
    return if (clockwise) {
        (at + trailing) to (at + travelled)
    } else {
        (at - travelled) to (at - trailing)
    }
}

/** Draws the element's [shape] outline as a stroke from its start up to the progress. */
internal class TraceProgressRunner(
    private val shape: Shape,
    private val color: Color,
    private val width: Dp,
    private val brush: Brush? = null,
    private val clockwise: Boolean = true,
    private val start: Float = 0f,
    private val tail: Float = 1f,
    private val cap: StrokeCap = StrokeCap.Round,
) : ProgressRunner() {
    private val measure = PathMeasure()
    private val segment = Path()
    private var path = Path()
    private var measuredSize = Size.Unspecified
    private var measuredInset = -1f
    private var length = 0f

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val p = progress().coerceIn(0f, 1f)
        if (p <= 0f) return
        val stroke = width.toPx()
        measure(stroke)
        if (length <= 0f) return
        val (from, to) = traceStretch(length, p, start, clockwise, tail)
        if (to - from >= length) {
            // The whole outline, closed, so its two ends join without a seam.
            paint(path, Stroke(stroke))
            return
        }
        val spans = wrappedSpans(from, to, length)
        for (k in spans.indices step 2) {
            segment.reset()
            measure.getSegment(spans[k], spans[k + 1], segment, startWithMoveTo = true)
            paint(segment, Stroke(stroke, cap = cap))
        }
    }

    private fun DrawScope.paint(
        outline: Path,
        style: Stroke,
    ) {
        if (brush != null) drawPath(outline, brush, style = style) else drawPath(outline, color, style = style)
    }

    /** Measures the outline for this scope's size and a stroke [strokeWidth] wide, if that changed. */
    private fun DrawScope.measure(strokeWidth: Float) {
        val inset = strokeWidth / 2
        if (size == measuredSize && inset == measuredInset) return
        measuredSize = size
        measuredInset = inset
        val inner = Size((size.width - 2 * inset).coerceAtLeast(0f), (size.height - 2 * inset).coerceAtLeast(0f))
        path = outlinePath(shape.createOutline(inner, layoutDirection, this))
        path.translate(Offset(inset, inset))
        measure.setPath(path, forceClosed = true)
        length = measure.length
    }
}
