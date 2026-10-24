package com.jantursky.kapow.particles

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A closed range of floats, picked from uniformly. */
internal data class Spread(
    val min: Float,
    val max: Float,
) {
    constructor(value: Float) : this(value, value)

    fun pick(random: Random): Float = if (min == max) min else min + random.nextFloat() * (max - min)
}

/**
 * How one emission launches its particles. Positions and speeds are in pixels, angles in degrees clockwise from
 * pointing right (so -90 is straight up), times in seconds.
 */
internal data class Emission(
    val count: Int,
    val angle: Spread,
    val speed: Spread,
    val lifetime: Spread,
    val gravity: Float = 0f,
    /** Fraction of velocity kept after one second. 1 keeps it all. */
    val drag: Float = 1f,
    val spin: Spread = Spread(0f),
    val rotation: Spread = Spread(0f),
    val scale: Spread = Spread(1f),
    /** Where particles start, relative to the emission origin. */
    val offsetX: Spread = Spread(0f),
    val offsetY: Spread = Spread(0f),
    /** Particles start one after another over this many seconds instead of all at once. */
    val stagger: Float = 0f,
    /** A sideways sway on top of the motion: its width in pixels and how many times a second it swings. */
    val wobble: Float = 0f,
    val wobbleRate: Float = 1f,
)

/**
 * A fixed set of particles from one [Emission], stepped by elapsed time. Plain arrays, no allocation per frame,
 * and the same seed and frame times always give the same positions.
 */
internal class ParticleSystem(
    private val emission: Emission,
    seed: Int,
) {
    val count: Int = emission.count
    val x = FloatArray(count)
    val y = FloatArray(count)
    private val vx = FloatArray(count)
    private val vy = FloatArray(count)
    val rotation = FloatArray(count)
    private val spin = FloatArray(count)
    val scale = FloatArray(count)
    private val lifetime = FloatArray(count)
    private val delay = FloatArray(count)
    private val age = FloatArray(count)
    private val phase = FloatArray(count)

    init {
        val random = Random(seed)
        for (i in 0 until count) {
            val angle = emission.angle.pick(random) * PI.toFloat() / 180f
            val speed = emission.speed.pick(random)
            x[i] = emission.offsetX.pick(random)
            y[i] = emission.offsetY.pick(random)
            vx[i] = cos(angle) * speed
            vy[i] = sin(angle) * speed
            rotation[i] = emission.rotation.pick(random)
            spin[i] = emission.spin.pick(random)
            scale[i] = emission.scale.pick(random)
            lifetime[i] = emission.lifetime.pick(random)
            delay[i] = if (count > 1) emission.stagger * i / (count - 1) else 0f
            if (emission.wobble != 0f) phase[i] = random.nextFloat() * 2 * PI.toFloat()
        }
    }

    /** Whether particle [i] has started and not yet expired. */
    fun isAlive(i: Int): Boolean = age[i] >= delay[i] && age[i] - delay[i] < lifetime[i]

    /** Where particle [i] is drawn horizontally: its position plus the sway. */
    fun drawnX(i: Int): Float {
        if (emission.wobble == 0f) return x[i]
        val time = (age[i] - delay[i]).coerceAtLeast(0f)
        return x[i] + emission.wobble * sin(2 * PI.toFloat() * emission.wobbleRate * time + phase[i])
    }

    /** How far particle [i] is through its life, from 0 to 1. */
    fun progress(i: Int): Float = ((age[i] - delay[i]) / lifetime[i]).coerceIn(0f, 1f)

    /** Advances every particle by [seconds]. Returns whether any particle is still waiting or alive. */
    fun step(seconds: Float): Boolean {
        var remaining = false
        val keep = if (emission.drag >= 1f) 1f else emission.drag.pow(seconds)
        for (i in 0 until count) {
            val before = age[i]
            age[i] += seconds
            val active = (age[i] - delay[i]).coerceIn(0f, lifetime[i]) - (before - delay[i]).coerceIn(0f, lifetime[i])
            if (active > 0f) {
                vy[i] += emission.gravity * active
                vx[i] *= keep
                vy[i] *= keep
                x[i] += vx[i] * active
                y[i] += vy[i] * active
                rotation[i] += spin[i] * active
            }
            if (age[i] - delay[i] < lifetime[i]) remaining = true
        }
        return remaining
    }
}

private fun Float.pow(exponent: Float): Float = kotlin.math.exp(kotlin.math.ln(this) * exponent)
