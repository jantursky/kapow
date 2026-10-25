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
 * How far each of the blob's [count] points reaches at [progress], as a factor of its radius. Each point swells and
 * shrinks at its own pace, between 1 - [wobble] and 1 + [wobble], so the outline keeps moving while it grows. The
 * pace and phase of each point come from [seed].
 */
internal fun blobFactors(
    progress: Float,
    count: Int = BLOB_POINTS,
    wobble: Float = BLOB_WOBBLE,
    seed: Int = BLOB_SEED,
): FloatArray =
    FloatArray(count) { i ->
        val phase = 2 * PI.toFloat() * cellHash(i, 0, seed)
        val pace = 1f + cellHash(i, 1, seed)
        1f + wobble * sin(phase + 2 * PI.toFloat() * pace * progress)
    }

/**
 * The blob's radius at full progress around [point] in [size]. The smooth outline runs through the midpoints
 * between its [count] points, which can sit closer in than the points by cos(π / [count]), once for the midpoint
 * and once for the curve, and every point can shrink by [wobble]. Grown to this, it still covers every corner.
 */
internal fun blobReach(
    size: Size,
    point: Offset,
    count: Int = BLOB_POINTS,
    wobble: Float = BLOB_WOBBLE,
): Float {
    val inset = cos(PI.toFloat() / count)
    return farthestCorner(size, point) / ((1f - wobble) * inset * inset)
}

/** The blob's outline at [progress], grown from [origin]: a smooth closed curve through its wobbling points. */
internal fun blobPath(
    size: Size,
    origin: TransformOrigin,
    progress: Float,
    count: Int = BLOB_POINTS,
    wobble: Float = BLOB_WOBBLE,
    seed: Int = BLOB_SEED,
): Path {
    val point = originPoint(size, origin)
    val p = progress.coerceIn(0f, 1f)
    val radius = blobReach(size, point, count, wobble) * p
    val factors = blobFactors(p, count, wobble, seed)
    // The whole blob turns a little as it grows.
    val turn = BLOB_TURN * p
    val xs = FloatArray(count)
    val ys = FloatArray(count)
    for (i in 0 until count) {
        val angle = turn + 2 * PI.toFloat() * i / count
        xs[i] = point.x + radius * factors[i] * cos(angle)
        ys[i] = point.y + radius * factors[i] * sin(angle)
    }
    return Path().apply {
        val last = count - 1
        moveTo((xs[last] + xs[0]) / 2, (ys[last] + ys[0]) / 2)
        for (i in 0 until count) {
            val next = (i + 1) % count
            quadraticTo(xs[i], ys[i], (xs[i] + xs[next]) / 2, (ys[i] + ys[next]) / 2)
        }
        close()
    }
}

internal const val BLOB_POINTS = 9
internal const val BLOB_WOBBLE = 0.25f
internal const val BLOB_SEED = 53
private const val BLOB_TURN = 0.6f

/** An organic, wobbling blob that grows from an origin until it covers the content, or shrinks back into it. */
internal class BlobMask(
    private val kind: TransitionKind.Blob,
) : TransitionPart() {
    override fun ContentDrawScope.draw(progress: Float) {
        drawContent()
        cutOutside(blobPath(size, kind.origin, progress, kind.points, kind.wobble, kind.seed))
    }
}
