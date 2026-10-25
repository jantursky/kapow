package com.jantursky.kapow.tilt

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.hypot

/** The rotation of the tilted element about the horizontal axis ([rotationX]) and the vertical one ([rotationY]), in degrees. */
internal data class TiltAngles(
    val rotationX: Float,
    val rotationY: Float,
) {
    companion object {
        val Flat = TiltAngles(0f, 0f)
    }
}

/**
 * The pointer at [position] in an element of [size], as -1 to 1 on both axes: -1 at the left and top edges, 0 in the
 * middle and 1 at the right and bottom edges. A position outside the element counts as on its edge.
 */
internal fun normalizedPointer(
    position: Offset,
    size: Size,
): Offset {
    if (size.width <= 0f || size.height <= 0f) return Offset.Zero
    val x = (position.x / size.width * 2f - 1f).coerceIn(-1f, 1f)
    val y = (position.y / size.height * 2f - 1f).coerceIn(-1f, 1f)
    return Offset(x, y)
}

/**
 * The angles for a pointer at [position]: the side the pointer is on dips away from the viewer, by an angle in
 * proportion to how far the pointer is from the middle and by [maxAngle] at the edge.
 */
internal fun tiltAngles(
    position: Offset,
    size: Size,
    maxAngle: Float,
): TiltAngles {
    val p = normalizedPointer(position, size)
    // A pointer at the right turns the right edge away: that is a positive turn about the vertical axis.
    // A pointer at the top turns the top edge away: that is a positive turn about the horizontal axis.
    return TiltAngles(rotationX = 0f - p.y * maxAngle, rotationY = p.x * maxAngle)
}

/** How strongly the element is tilted, from 0 when flat to 1 at [maxAngle] on both axes' combined length. */
internal fun tiltStrength(
    angles: TiltAngles,
    maxAngle: Float,
): Float = if (maxAngle <= 0f) 0f else (hypot(angles.rotationX, angles.rotationY) / maxAngle).coerceIn(0f, 1f)

/**
 * Where the sheen is centered: mirrored through the middle from where the pointer is, so a tilt toward the top right
 * puts the light at the bottom left, the way a glossy card catches light.
 */
internal fun sheenCenter(
    angles: TiltAngles,
    maxAngle: Float,
    size: Size,
): Offset {
    if (maxAngle <= 0f) return Offset(size.width / 2f, size.height / 2f)
    val nx = (angles.rotationY / maxAngle).coerceIn(-1f, 1f)
    val ny = (-angles.rotationX / maxAngle).coerceIn(-1f, 1f)
    return Offset((1f - nx) / 2f * size.width, (1f - ny) / 2f * size.height)
}
