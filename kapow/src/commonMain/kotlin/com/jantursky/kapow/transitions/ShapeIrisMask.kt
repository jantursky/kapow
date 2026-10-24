package com.jantursky.kapow.transitions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.TransitionKind
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * The radius of the largest circle around [center] that the closed outline through the points ([xs], [ys]) holds:
 * the distance to its nearest point, or 0 when [center] is outside it.
 */
internal fun inradius(
    xs: FloatArray,
    ys: FloatArray,
    center: Offset,
): Float {
    if (!encloses(xs, ys, center)) return 0f
    var nearest = Float.MAX_VALUE
    for (i in xs.indices) {
        val j = (i + 1) % xs.size
        nearest = min(nearest, segmentDistance(center, xs[i], ys[i], xs[j], ys[j]))
    }
    return nearest
}

/** Whether [point] is inside the closed outline through ([xs], [ys]), by the even-odd rule. */
internal fun encloses(
    xs: FloatArray,
    ys: FloatArray,
    point: Offset,
): Boolean {
    var inside = false
    var j = xs.size - 1
    for (i in xs.indices) {
        if ((ys[i] > point.y) != (ys[j] > point.y) &&
            point.x < xs[i] + (point.y - ys[i]) * (xs[j] - xs[i]) / (ys[j] - ys[i])
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

private fun segmentDistance(
    p: Offset,
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float,
): Float {
    val dx = bx - ax
    val dy = by - ay
    val length = dx * dx + dy * dy
    val t = if (length == 0f) 0f else (((p.x - ax) * dx + (p.y - ay) * dy) / length).coerceIn(0f, 1f)
    return hypot(p.x - (ax + t * dx), p.y - (ay + t * dy))
}

/**
 * The side of the square box the shape is drawn in at [progress], growing around [point] inside [size]. At 1 the
 * circle the shape holds, [ratio] of its side, reaches the farthest corner, so the shape covers everything.
 */
internal fun shapeIrisSide(
    size: Size,
    point: Offset,
    ratio: Float,
    progress: Float,
): Float = farthestCorner(size, point) / ratio * progress.coerceIn(0f, 1f)

/** [inradius] of an [outline] drawn in a [side] px square, as a share of the side. Never below [MIN_RATIO]. */
internal fun outlineRatio(
    outline: Outline,
    side: Float,
): Float {
    val center = Offset(side / 2, side / 2)
    val radius =
        when (outline) {
            is Outline.Rectangle -> {
                outline.rect.run { sideDistance(center, left, top, right, bottom) }
            }

            // Corners never curve in past the largest circle that fits the sides, so that circle is the one held.
            is Outline.Rounded -> {
                outline.roundRect.run { sideDistance(center, left, top, right, bottom) }
            }

            is Outline.Generic -> {
                val measure = PathMeasure().apply { setPath(outline.path, true) }
                val xs = FloatArray(SAMPLES)
                val ys = FloatArray(SAMPLES)
                for (i in 0 until SAMPLES) {
                    val at = measure.getPosition(measure.length * i / SAMPLES)
                    xs[i] = at.x
                    ys[i] = at.y
                }
                inradius(xs, ys, center)
            }
        }
    return max(MIN_RATIO, radius / side)
}

private fun sideDistance(
    center: Offset,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
): Float = min(min(center.x - left, right - center.x), min(center.y - top, bottom - center.y))

private const val SAMPLES = 256

/** A shape that holds almost nothing around its center would have to grow without end; it stops at this. */
internal const val MIN_RATIO = 0.05f

/** A [TransitionKind.ShapeIris]: any shape that grows from an origin until it covers the content. */
internal class ShapeIrisMask(
    private val kind: TransitionKind.ShapeIris,
) : TransitionPart() {
    private var measured: Pair<Size, Float>? = null

    override fun ContentDrawScope.draw(progress: Float) {
        drawContent()
        val point = originPoint(size, kind.origin)
        val side = shapeIrisSide(size, point, ratio(size, layoutDirection, this), progress)
        val path = Path().apply { addOutline(kind.shape.createOutline(Size(side, side), layoutDirection, this@draw)) }
        path.translate(Offset(point.x - side / 2, point.y - side / 2))
        cutOutside(path)
    }

    /** How much of its box the shape holds, measured once per element size. */
    private fun ratio(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Float {
        measured?.let { (at, ratio) -> if (at == size) return ratio }
        val side = max(1f, max(size.width, size.height))
        val ratio = outlineRatio(kind.shape.createOutline(Size(side, side), layoutDirection, density), side)
        measured = size to ratio
        return ratio
    }
}
