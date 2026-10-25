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
import androidx.compose.ui.graphics.drawscope.scale
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

/**
 * How far slat [index] of [count] is open at [progress]. Staggered slats open from the first to the last, or from
 * the last to the first when [reverse]. [amount] is how far the start of the last slat trails the first: 0 opens
 * them together, 1 (the default) spreads the starts over half the progress, more spreads them further.
 */
internal fun slatOpening(
    index: Int,
    count: Int,
    progress: Float,
    staggered: Boolean,
    amount: Float = 1f,
    reverse: Boolean = false,
): Float {
    if (!staggered) return progress.coerceIn(0f, 1f)
    val place = (if (reverse) count - 1 - index else index).toFloat() / count
    if (amount == 1f) return (2 * progress - 1 + (1 - place)).coerceIn(0f, 1f)
    return (progress * (1 + amount) - amount * place).coerceIn(0f, 1f)
}

/**
 * How hidden a clock with a soft edge is at [angle], in degrees clockwise from 12 o'clock: 0 shown, 1 hidden. The
 * sweep has reached [sweep], and the fade from shown to hidden is [softAngle] wide and ends there.
 */
internal fun clockMaskAlpha(
    angle: Float,
    sweep: Float,
    softAngle: Float,
): Float = ((angle - (sweep - softAngle)) / softAngle).coerceIn(0f, 1f)

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
    counterClockwise: Boolean = false,
): Path {
    val point = originPoint(size, origin)
    val sweep = clockSweep(progress)
    val radius = farthestCorner(size, point) + 1f
    return Path().apply {
        if (sweep >= 360f) {
            addRect(Rect(Offset.Zero, size))
        } else if (sweep > 0f) {
            moveTo(point.x, point.y)
            arcTo(Rect(point, radius), -90f, if (counterClockwise) -sweep else sweep, forceMoveTo = false)
            close()
        }
    }
}

/** Clears everything outside an iris at [progress] whose edge fades over [soft] px, or is sharp when [soft] is 0. */
internal fun DrawScope.cutIris(
    origin: TransformOrigin,
    soft: Float,
    progress: Float,
) {
    if (soft <= 0f) {
        cutOutside(irisPath(size, origin, progress))
        return
    }
    val point = originPoint(size, origin)
    // The edge travels [soft] further than a sharp one, so the fade has left the content when the iris is done.
    val outer = (farthestCorner(size, point) + soft) * progress.coerceIn(0f, 1f)
    if (outer <= 0f) {
        drawRect(Color.Black, blendMode = BlendMode.DstOut)
        return
    }
    val inner = (outer - soft).coerceAtLeast(0f)
    drawRect(
        Brush.radialGradient(inner / outer to Color.Transparent, 1f to Color.Black, center = point, radius = outer),
        blendMode = BlendMode.DstOut,
    )
}

/** Clears everything outside a clock sweep at [progress] whose edge fades over [soft] px, or is sharp at 0. */
internal fun DrawScope.cutClock(
    origin: TransformOrigin,
    soft: Float,
    counterClockwise: Boolean,
    progress: Float,
) {
    if (soft <= 0f) {
        cutOutside(clockPath(size, origin, progress, counterClockwise))
        return
    }
    val point = originPoint(size, origin)
    val radius = farthestCorner(size, point) + 1f
    // The fade is [soft] px wide at the farthest corner; closer in it is narrower.
    val softAngle = (soft / radius * 180f / PI.toFloat()).coerceIn(1f, 180f)
    val end = clockSweep(progress) / 360f * (360f + softAngle)

    fun stop(angle: Float): Pair<Float, Color> {
        val at = angle.coerceIn(0f, 360f)
        return at / 360f to Color.Black.copy(alpha = clockMaskAlpha(at, end, softAngle))
    }
    val brush = Brush.sweepGradient(stop(0f), stop(end - softAngle), stop(end), stop(360f), center = point)
    // A sweep gradient starts at 3 o'clock; turn it to 12, and mirror it for a counterclockwise sweep.
    scale(if (counterClockwise) -1f else 1f, 1f, pivot = point) {
        rotate(-90f, pivot = point) {
            drawRect(
                brush,
                topLeft = Offset(-radius, -radius),
                size =
                    Size(
                        size.width + radius * 2,
                        size.height + radius * 2,
                    ),
                blendMode = BlendMode.DstOut,
            )
        }
    }
}

internal fun blindsPath(
    size: Size,
    slat: Float,
    orientation: BlindsOrientation,
    staggered: Boolean,
    progress: Float,
    slats: Int = 0,
    staggerAmount: Float = 1f,
    reverse: Boolean = false,
): Path {
    val extent = if (orientation == BlindsOrientation.Horizontal) size.height else size.width
    val count = if (slats > 0) slats else max(1, ceil(extent / slat).toInt())
    val slat = if (slats > 0) extent / slats else slat
    return Path().apply {
        for (i in 0 until count) {
            val open = slatOpening(i, count, progress, staggered, staggerAmount, reverse)
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

/**
 * Clears everything ahead of a wipe edge moving at [angle]; draws a streak of [glare] along the edge if set. With a
 * [soft] edge the cut fades from shown to hidden over that many px; the edge then travels that much further, so the
 * fade has left the content at the end. The streak is [glareWidth] px wide (a share of the element's size when
 * NaN) and [intensity] times as strong.
 */
internal fun DrawScope.cutWipe(
    angle: Float,
    glare: Color?,
    progress: Float,
    soft: Float = 0f,
    glareWidth: Float = Float.NaN,
    intensity: Float = 1f,
) {
    val reach = hypot(size.width, size.height) / 2
    val edge =
        if (soft >
            0f
        ) {
            -reach - soft + (2 * reach + soft) * progress.coerceIn(0f, 1f)
        } else {
            wipeEdge(reach, progress)
        }
    rotate(angle, pivot = center) {
        // In this rotated frame the edge is vertical and moves right; everything to its right is still hidden.
        if (soft > 0f) {
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.Black),
                    startX = center.x + edge,
                    endX = center.x + edge + soft,
                ),
                topLeft = Offset(center.x + edge, center.y - reach * 2),
                size = Size(reach * 3, reach * 4),
                blendMode = BlendMode.DstOut,
            )
        } else {
            drawRect(
                Color.Black,
                topLeft = Offset(center.x + edge, center.y - reach * 2),
                size = Size(reach * 3, reach * 4),
                blendMode = BlendMode.DstOut,
            )
        }
        if (glare != null) {
            val width = if (glareWidth.isNaN()) reach * 0.35f else glareWidth
            val strength = sin(PI.toFloat() * progress.coerceIn(0f, 1f)) * intensity
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
