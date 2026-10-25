package com.jantursky.kapow.internal

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.EdgeSide
import com.jantursky.kapow.EmissionArea
import com.jantursky.kapow.ParticlePhysics
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.Spread

/**
 * [base] with the parts [this] sets laid over it. Speeds and gravity come in dp, so [density] turns them into
 * pixels; [area] is the element's size in pixels, for [EmissionArea].
 */
internal fun ParticlePhysics.applyTo(
    base: Emission,
    density: Float,
    area: Size,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
): Emission {
    val angle =
        if (direction == null && spread == null) {
            base.angle
        } else {
            val middle = direction ?: ((base.angle.min + base.angle.max) / 2f)
            val half = (spread ?: (base.angle.max - base.angle.min)) / 2f
            Spread(middle - half, middle + half)
        }
    val (offsetX, offsetY) = emission?.offsets(area, layoutDirection) ?: (base.offsetX to base.offsetY)
    return base.copy(
        angle = angle,
        speed = speed?.let { Spread(it.start * density, it.endInclusive * density) } ?: base.speed,
        lifetime = lifetime?.let { Spread(it.start, it.endInclusive) } ?: base.lifetime,
        gravity = gravity?.let { it * density } ?: base.gravity,
        drag = drag ?: base.drag,
        spin = spin?.let { Spread(it.start, it.endInclusive) } ?: base.spin,
        scale = size?.let { Spread(base.scale.min * it.start, base.scale.max * it.endInclusive) } ?: base.scale,
        offsetX = offsetX,
        offsetY = offsetY,
    )
}

/** The horizontal and vertical start offsets of an [EmissionArea] centered on the origin. */
internal fun EmissionArea.offsets(
    area: Size,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
): Pair<Spread, Spread> {
    val halfW = area.width / 2f
    val halfH = area.height / 2f
    return when (this) {
        EmissionArea.Point -> {
            Spread(0f) to Spread(0f)
        }

        EmissionArea.Rect -> {
            Spread(-halfW, halfW) to Spread(-halfH, halfH)
        }

        is EmissionArea.Edge -> {
            val rtl = layoutDirection == LayoutDirection.Rtl
            when (side) {
                EdgeSide.Top -> Spread(-halfW, halfW) to Spread(-halfH)
                EdgeSide.Bottom -> Spread(-halfW, halfW) to Spread(halfH)
                EdgeSide.Start -> Spread(if (rtl) halfW else -halfW) to Spread(-halfH, halfH)
                EdgeSide.End -> Spread(if (rtl) -halfW else halfW) to Spread(-halfH, halfH)
            }
        }
    }
}

/** How visible a particle is at [life] (0 to 1), using [ParticlePhysics.fade] when set, otherwise [default]. */
internal fun ParticlePhysics.opacityCurve(default: (Float) -> Float): (Float) -> Float {
    val easing = fade ?: return default
    return { 1f - easing.transform(it.coerceIn(0f, 1f)) }
}

/** The size factor at [life] (0 to 1), from [ParticlePhysics.startScale] to [ParticlePhysics.endScale] when set. */
internal fun ParticlePhysics.growthCurve(default: (Float) -> Float): (Float) -> Float {
    if (startScale == null && endScale == null) return default
    val from = startScale ?: default(0f)
    val to = endScale ?: default(1f)
    return { from + (to - from) * it.coerceIn(0f, 1f) }
}
