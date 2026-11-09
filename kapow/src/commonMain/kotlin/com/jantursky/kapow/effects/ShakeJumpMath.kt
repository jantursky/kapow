package com.jantursky.kapow.effects

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * The shake's swing at [t] (0 to 1 over the whole effect), from -1 to 1: [swings] full back-and-forth swings whose
 * size falls as `(1 - t)^decay`, so it starts and ends at 0.
 */
internal fun shakeOffset(
    t: Float,
    swings: Int,
    decay: Float,
): Float {
    val x = t.coerceIn(0f, 1f)
    if (x >= 1f) return 0f
    return sin(2f * PI.toFloat() * swings * x) * (1f - x).pow(decay)
}

/** The share of each hop spent in the air; the rest is the landing squash. */
private const val AIRBORNE = 0.8f

/** Where [t] (0 to 1 over all [hops]) falls: the hop it is in and how far through that hop, from 0 to 1. */
private fun hopAt(
    t: Float,
    hops: Int,
): Float {
    val x = t.coerceIn(0f, 1f) * hops
    val whole = x.toInt().coerceAtMost(hops - 1)
    return if (t >= 1f) 1f else x - whole
}

/**
 * How high the element is at [t] (0 to 1 over all [hops]), from 0 on the ground to 1 at the top of a hop. Each hop is
 * a parabola over the first [AIRBORNE] of its share of the time and rests on the ground for the remainder.
 */
internal fun jumpHeight(
    t: Float,
    hops: Int = 1,
): Float {
    val u = hopAt(t, hops)
    if (u >= AIRBORNE) return 0f
    val v = u / AIRBORNE
    return 4f * v * (1f - v)
}

/**
 * How squashed the element is at [t], from 0 (its own shape) to 1 (most flattened). It builds right after each
 * landing and has recovered by the end of the hop, so the last hop ends at rest.
 */
internal fun jumpSquash(
    t: Float,
    hops: Int = 1,
): Float {
    val u = hopAt(t, hops)
    if (u <= AIRBORNE || u >= 1f) return 0f
    return sin(PI.toFloat() * (u - AIRBORNE) / (1f - AIRBORNE))
}
