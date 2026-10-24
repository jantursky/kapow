// The iris radius reaching the farthest corner, the clock sweep running 5 % ahead so it closes fully, and the
// staggered blinds formula are derived from Pow (https://github.com/EmergeTools/Pow), MIT, see NOTICE.
package com.jantursky.kapow.transitions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.jantursky.kapow.BlindsOrientation
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** The point [origin] picks inside [size]. */
internal fun originPoint(
    size: Size,
    origin: TransformOrigin,
): Offset = Offset(size.width * origin.pivotFractionX, size.height * origin.pivotFractionY)

/** The distance from [point] to the farthest corner of [size], so a circle of that radius covers it all. */
internal fun farthestCorner(
    size: Size,
    point: Offset,
): Float =
    max(
        max(hypot(point.x, point.y), hypot(size.width - point.x, point.y)),
        max(
            hypot(
                point.x,
                size.height - point.y,
            ),
            hypot(size.width - point.x, size.height - point.y),
        ),
    )

/** The iris radius at [progress]. */
internal fun irisRadius(
    size: Size,
    point: Offset,
    progress: Float,
): Float = farthestCorner(size, point) * progress.coerceIn(0f, 1f)

/** The clock sweep in degrees at [progress]. It runs a little ahead so the last sliver closes before the end. */
internal fun clockSweep(progress: Float): Float = (progress * 1.05f).coerceIn(0f, 1f) * 360f

/** How far slat [index] of [count] is open at [progress]. Staggered slats open from the first to the last. */
internal fun slatOpening(
    index: Int,
    count: Int,
    progress: Float,
    staggered: Boolean,
): Float {
    if (!staggered) return progress.coerceIn(0f, 1f)
    return (2 * progress - 1 + (1 - index.toFloat() / count)).coerceIn(0f, 1f)
}

/** How far the wipe edge has travelled, from -[reach] to +[reach] along its direction through the center. */
internal fun wipeEdge(
    reach: Float,
    progress: Float,
): Float = -reach + 2 * reach * progress.coerceIn(0f, 1f)

internal fun irisPath(
    size: Size,
    origin: TransformOrigin,
    progress: Float,
): Path {
    val point = originPoint(size, origin)
    return Path().apply { addOval(Rect(point, irisRadius(size, point, progress))) }
}

internal fun clockPath(
    size: Size,
    origin: TransformOrigin,
    progress: Float,
): Path {
    val point = originPoint(size, origin)
    val sweep = clockSweep(progress)
    val radius = farthestCorner(size, point) + 1f
    return Path().apply {
        if (sweep >= 360f) {
            addRect(Rect(Offset.Zero, size))
        } else if (sweep > 0f) {
            moveTo(point.x, point.y)
            arcTo(Rect(point, radius), -90f, sweep, forceMoveTo = false)
            close()
        }
    }
}

internal fun blindsPath(
    size: Size,
    slat: Float,
    orientation: BlindsOrientation,
    staggered: Boolean,
    progress: Float,
): Path {
    val extent = if (orientation == BlindsOrientation.Horizontal) size.height else size.width
    val count = max(1, ceil(extent / slat).toInt())
    return Path().apply {
        for (i in 0 until count) {
            val open = slatOpening(i, count, progress, staggered)
            if (open <= 0f) continue
            val center = (i + 0.5f) * slat
            val half = slat * open / 2
            addRect(
                if (orientation == BlindsOrientation.Horizontal) {
                    Rect(0f, center - half, size.width, center + half)
                } else {
                    Rect(center - half, 0f, center + half, size.height)
                },
            )
        }
    }
}

/** Clears everything outside [visible]. The caller draws the content offscreen first. */
internal fun DrawScope.cutOutside(visible: Path) {
    val outside = Path().apply { addRect(Rect(Offset.Zero, size).inflate(1f)) }
    drawPath(Path.combine(PathOperation.Difference, outside, visible), Color.Black, blendMode = BlendMode.DstOut)
}

/** Clears everything ahead of a wipe edge moving at [angle]; draws a streak of [glare] along the edge if set. */
internal fun DrawScope.cutWipe(
    angle: Float,
    glare: Color?,
    progress: Float,
) {
    val reach = hypot(size.width, size.height) / 2
    val edge = wipeEdge(reach, progress)
    rotate(angle, pivot = center) {
        // In this rotated frame the edge is vertical and moves right; everything to its right is still hidden.
        drawRect(
            Color.Black,
            topLeft = Offset(center.x + edge, center.y - reach * 2),
            size = Size(reach * 3, reach * 4),
            blendMode = BlendMode.DstOut,
        )
        if (glare != null) {
            val width = reach * 0.35f
            val strength = sin(PI.toFloat() * progress.coerceIn(0f, 1f))
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, glare.copy(alpha = glare.alpha * strength), Color.Transparent),
                    startX = center.x + edge - width,
                    endX = center.x + edge,
                ),
                topLeft = Offset(center.x + edge - width, center.y - reach * 2),
                size = Size(width, reach * 4),
                blendMode = BlendMode.SrcAtop,
            )
        }
    }
}
