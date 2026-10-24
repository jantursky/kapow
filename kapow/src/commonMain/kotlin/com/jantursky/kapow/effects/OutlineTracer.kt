package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * The part of an outline of [length] that a stroke from [start] to [end] covers, clipped to the outline. Null when
 * nothing of it is on the outline.
 */
internal fun clipSpan(
    start: Float,
    end: Float,
    length: Float,
): ClosedFloatingPointRange<Float>? {
    val from = start.coerceIn(0f, length)
    val to = end.coerceIn(0f, length)
    return if (to > from) from..to else null
}

/**
 * The outline of a rounded rectangle, starting at the middle of its top edge and running clockwise, so a stroke
 * traced along it starts at 12 o'clock.
 */
internal fun roundRectPath(rect: RoundRect): Path =
    Path().apply {
        val left = rect.left
        val top = rect.top
        val right = rect.right
        val bottom = rect.bottom
        moveTo((left + right) / 2, top)
        lineTo(right - rect.topRightCornerRadius.x, top)
        corner(rect.topRightCornerRadius, right - 2 * rect.topRightCornerRadius.x, top, -90f)
        lineTo(right, bottom - rect.bottomRightCornerRadius.y)
        corner(
            rect.bottomRightCornerRadius,
            right - 2 * rect.bottomRightCornerRadius.x,
            bottom - 2 * rect.bottomRightCornerRadius.y,
            0f,
        )
        lineTo(left + rect.bottomLeftCornerRadius.x, bottom)
        corner(rect.bottomLeftCornerRadius, left, bottom - 2 * rect.bottomLeftCornerRadius.y, 90f)
        lineTo(left, top + rect.topLeftCornerRadius.y)
        corner(rect.topLeftCornerRadius, left, top, 180f)
        close()
    }

private fun Path.corner(
    radius: CornerRadius,
    left: Float,
    top: Float,
    startAngle: Float,
) {
    // A square corner has no arc; some platforms skip an arc in an empty oval, others draw a stray line.
    if (radius.x <= 0f || radius.y <= 0f) return
    arcTo(Rect(left, top, left + 2 * radius.x, top + 2 * radius.y), startAngle, 90f, false)
}

/** The path a stroke follows around [outline]. Rectangles and rounded rectangles start at 12 o'clock. */
internal fun outlinePath(outline: Outline): Path =
    when (outline) {
        is Outline.Rectangle -> roundRectPath(RoundRect(outline.rect))
        is Outline.Rounded -> roundRectPath(outline.roundRect)
        is Outline.Generic -> Path().apply { addPath(outline.path) }
    }

/**
 * Measures the outline of [shape] at the element's size, inset by half the stroke so the whole stroke stays inside
 * the element, and draws parts of it. The path is rebuilt only when the size or the stroke changes.
 */
internal class OutlineTracer(
    private val shape: Shape,
) {
    private val measure = PathMeasure()
    private val segment = Path()
    private var path = Path()
    private var measuredSize = Size.Unspecified
    private var measuredInset = -1f

    /** The outline's length in pixels at the last [measure]. */
    var length: Float = 0f
        private set

    /** Measures the outline for this scope's size and a stroke [strokeWidth] wide. */
    fun DrawScope.measure(strokeWidth: Float) {
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

    /** Draws the outline from [start] to [end] pixels along it, clipped to the outline. */
    fun DrawScope.drawSpan(
        start: Float,
        end: Float,
        color: Color,
        width: Float,
        alpha: Float = 1f,
        cap: StrokeCap = StrokeCap.Round,
    ) {
        val span = clipSpan(start, end, length) ?: return
        if (span.start <= 0f && span.endInclusive >= length) {
            // The whole outline, closed, so its two ends join without a seam.
            drawPath(path, color, alpha, Stroke(width))
            return
        }
        segment.reset()
        measure.getSegment(span.start, span.endInclusive, segment, startWithMoveTo = true)
        drawPath(segment, color, alpha, Stroke(width, cap = cap))
    }
}
