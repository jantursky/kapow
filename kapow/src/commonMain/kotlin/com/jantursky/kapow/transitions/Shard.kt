package com.jantursky.kapow.transitions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * One triangular piece of shattered content: its corners ([xs], [ys]) in the content's coordinates, its center,
 * and how it flies: [driftX] and [kick] as shares of the fall distance, [spin] in degrees over the whole fall.
 */
internal class Shard(
    val xs: FloatArray,
    val ys: FloatArray,
    val driftX: Float,
    val kick: Float,
    val spin: Float,
) {
    val center: Offset = Offset(xs.average().toFloat(), ys.average().toFloat())
}

/**
 * Breaks [size] into about [count] triangles: a grid of cells whose inner corners are moved at random, each cell
 * cut along one of its diagonals. The pieces tile the content exactly and are the same for the same arguments.
 * The pieces lean away from [impactX] sideways, and [seed] sets the pattern.
 */
internal fun shatterShards(
    size: Size,
    count: Int,
    seed: Int = SHATTER_SEED,
    impactX: Float = size.width / 2,
): List<Shard> {
    val cells = max(1, count / 2)
    val columns = max(1, sqrt(cells * size.width / max(1f, size.height)).roundToInt())
    val rows = max(1, (cells.toFloat() / columns).roundToInt())
    val cw = size.width / columns
    val ch = size.height / rows

    fun x(
        c: Int,
        r: Int,
    ): Float = c * cw + if (c in 1 until columns) (cellHash(c, r, seed) - 0.5f) * cw * 0.6f else 0f

    fun y(
        c: Int,
        r: Int,
    ): Float = r * ch + if (r in 1 until rows) (cellHash(c, r, seed + 1) - 0.5f) * ch * 0.6f else 0f

    val shards = ArrayList<Shard>(columns * rows * 2)
    for (r in 0 until rows) {
        for (c in 0 until columns) {
            val corners = listOf(c to r, c + 1 to r, c + 1 to r + 1, c to r + 1)
            // Cutting along one diagonal or the other keeps the cracks from lining up.
            val triangles =
                if (cellHash(c, r, seed + 2) < 0.5f) {
                    listOf(listOf(0, 1, 2), listOf(0, 2, 3))
                } else {
                    listOf(listOf(0, 1, 3), listOf(1, 2, 3))
                }
            for (triangle in triangles) {
                val xs = FloatArray(3) { corners[triangle[it]].let { (cc, rr) -> x(cc, rr) } }
                val ys = FloatArray(3) { corners[triangle[it]].let { (cc, rr) -> y(cc, rr) } }
                val i = shards.size
                val side = (xs.average().toFloat() - impactX) / max(1f, size.width)
                shards +=
                    Shard(
                        xs,
                        ys,
                        driftX = side * 0.3f + (cellHash(i, 0, seed + 3) - 0.5f) * 0.15f,
                        kick = 0.15f + 0.25f * cellHash(i, 1, seed + 3),
                        spin = (cellHash(i, 2, seed + 3) - 0.5f) * 2 * MAX_SPIN,
                    )
            }
        }
    }
    return shards
}

/**
 * Where [shard] is at [t] (0 to 1 over the exit), as an offset from where it started, for a fall of [fall] px. For
 * the first [CRACK] of the time the pieces only part along the cracks, by up to [gap] px; then they are thrown
 * up a little and fall. [gravity] is a factor on how hard they fall; the pieces part away from [contentCenter], the
 * point of impact.
 */
internal fun shardOffset(
    shard: Shard,
    t: Float,
    contentCenter: Offset,
    fall: Float,
    gap: Float,
    gravity: Float = 1f,
): Offset {
    val away = shard.center - contentCenter
    val distance = away.getDistance()
    val apart = if (distance > 0f) away / distance * gap * (t / CRACK).coerceAtMost(1f) else Offset.Zero
    val u = ((t - CRACK) / (1f - CRACK)).coerceIn(0f, 1f)
    return apart + Offset(shard.driftX * u * fall, (GRAVITY * gravity * u * u - shard.kick * u) * fall)
}

/** How far [shard] has turned at [t], in degrees; [spin] is a factor on how far. It turns faster as it falls faster. */
internal fun shardRotation(
    shard: Shard,
    t: Float,
    spin: Float = 1f,
): Float {
    val u = ((t - CRACK) / (1f - CRACK)).coerceIn(0f, 1f)
    return shard.spin * spin * u * u
}

/** The pieces' opacity at [t]: whole while they fly, then fading out over the last part of the fall. */
internal fun shatterAlpha(t: Float): Float {
    val f = ((t - FADE_FROM) / (1f - FADE_FROM)).coerceIn(0f, 1f)
    return 1f - f * f * (3 - 2 * f)
}

internal const val CRACK = 0.12f
private const val GRAVITY = 1.8f
private const val MAX_SPIN = 160f
private const val FADE_FROM = 0.55f
internal const val SHATTER_SEED = 61
