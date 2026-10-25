package com.jantursky.kapow.internal

import androidx.compose.ui.geometry.Offset
import com.jantursky.kapow.ParticlePhysics
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.Spread
import kotlin.math.pow
import kotlin.time.Duration

/**
 * This emission played at [speed] times the pace: the same paths, covered in 1 / [speed] of the time. Spawn rates
 * are the runner's business.
 */
internal fun Emission.atSpeed(speed: Float): Emission {
    if (speed == 1f) return this
    return copy(
        speed = Spread(this.speed.min * speed, this.speed.max * speed),
        lifetime = Spread(lifetime.min / speed, lifetime.max / speed),
        gravity = gravity * speed * speed,
        gravityX = gravityX * speed * speed,
        drag = drag.pow(speed),
        spin = Spread(spin.min * speed, spin.max * speed),
        stagger = stagger / speed,
        wobbleRate = wobbleRate * speed,
    )
}

/**
 * Where a continuous emitter starts its particles, relative to the element's top left corner: [fallback] unless
 * [ParticlePhysics.emission] is set, which spreads particles around the element's center.
 */
internal fun ParticlePhysics.emitterOrigin(
    width: Float,
    height: Float,
    fallback: Offset,
): Offset = if (emission != null) Offset(width / 2f, height / 2f) else fallback

/** The wait between two emissions that normally come every [interval], at [rate] times the pace. */
internal fun scaledInterval(
    interval: Duration,
    rate: Float,
): Duration = if (rate == 1f) interval else interval / rate.toDouble()

/** A pseudo-random index below [bound] for particle [index] of a run seeded with [seed]. */
internal fun scatter(
    seed: Int,
    index: Int,
    bound: Int,
): Int {
    var h = seed * 0x2545F491 + index * 0x9E3779B1.toInt()
    h = h xor (h ushr 15)
    h *= 0x2C1B3C6D
    h = h xor (h ushr 12)
    return (h and Int.MAX_VALUE) % bound.coerceAtLeast(1)
}
