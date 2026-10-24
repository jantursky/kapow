package com.jantursky.kapow.transitions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import com.jantursky.kapow.TransitionKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * How far each of the blob's [BLOB_POINTS] points reaches at [progress], as a factor of its radius. Each point
 * swells and shrinks at its own pace, between 1 - [BLOB_WOBBLE] and 1 + [BLOB_WOBBLE], so the outline keeps moving
 * while it grows.
 */
internal fun blobFactors(progress: Float): FloatArray =
    FloatArray(BLOB_POINTS) { i ->
        val phase = 2 * PI.toFloat() * cellHash(i, 0, BLOB_SEED)
        val pace = 1f + cellHash(i, 1, BLOB_SEED)
        1f + BLOB_WOBBLE * sin(phase + 2 * PI.toFloat() * pace * progress)
    }

/**
 * The blob's radius at full progress around [point] in [size]. The smooth outline runs through the midpoints
 * between its points, which can sit closer in than the points by cos(π / [BLOB_POINTS]), once for the midpoint and
 * once for the curve, and every point can shrink by [BLOB_WOBBLE]. Grown to this, it still covers every corner.
 */
internal fun blobReach(
    size: Size,
    point: Offset,
): Float {
    val inset = cos(PI.toFloat() / BLOB_POINTS)
    return farthestCorner(size, point) / ((1f - BLOB_WOBBLE) * inset * inset)
}

/** The blob's outline at [progress], grown from [origin]: a smooth closed curve through its wobbling points. */
internal fun blobPath(
    size: Size,
    origin: TransformOrigin,
    progress: Float,
): Path {
    val point = originPoint(size, origin)
    val p = progress.coerceIn(0f, 1f)
    val radius = blobReach(size, point) * p
    val factors = blobFactors(p)
    // The whole blob turns a little as it grows.
    val turn = BLOB_TURN * p
    val xs = FloatArray(BLOB_POINTS)
    val ys = FloatArray(BLOB_POINTS)
    for (i in 0 until BLOB_POINTS) {
        val angle = turn + 2 * PI.toFloat() * i / BLOB_POINTS
        xs[i] = point.x + radius * factors[i] * cos(angle)
        ys[i] = point.y + radius * factors[i] * sin(angle)
    }
    return Path().apply {
        val last = BLOB_POINTS - 1
        moveTo((xs[last] + xs[0]) / 2, (ys[last] + ys[0]) / 2)
        for (i in 0 until BLOB_POINTS) {
            val next = (i + 1) % BLOB_POINTS
            quadraticTo(xs[i], ys[i], (xs[i] + xs[next]) / 2, (ys[i] + ys[next]) / 2)
        }
        close()
    }
}

internal const val BLOB_POINTS = 9
internal const val BLOB_WOBBLE = 0.25f
private const val BLOB_TURN = 0.6f
private const val BLOB_SEED = 53

/** An organic, wobbling blob that grows from an origin until it covers the content, or shrinks back into it. */
internal class BlobMask(
    private val kind: TransitionKind.Blob,
) : TransitionPart() {
    override fun ContentDrawScope.draw(progress: Float) {
        drawContent()
        cutOutside(blobPath(size, kind.origin, progress))
    }
}
