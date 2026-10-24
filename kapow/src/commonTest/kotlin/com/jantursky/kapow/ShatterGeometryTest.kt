package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.jantursky.kapow.transitions.CRACK
import com.jantursky.kapow.transitions.Shard
import com.jantursky.kapow.transitions.shardOffset
import com.jantursky.kapow.transitions.shardRotation
import com.jantursky.kapow.transitions.shatterAlpha
import com.jantursky.kapow.transitions.shatterShards
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShatterGeometryTest {
    private val size = Size(200f, 100f)

    private fun Shard.area(): Float = abs((xs[1] - xs[0]) * (ys[2] - ys[0]) - (xs[2] - xs[0]) * (ys[1] - ys[0])) / 2

    @Test
    fun theShardsTileTheContent() {
        val shards = shatterShards(size, 24)
        assertTrue(shards.size in 20..28, "${shards.size} shards")
        assertEquals(size.width * size.height, shards.sumOf { it.area().toDouble() }.toFloat(), 1f)
        assertTrue(shards.all { s -> s.xs.all { it in 0f..200f } && s.ys.all { it in 0f..100f } })
        assertEquals(2, shatterShards(size, 2).size)
    }

    @Test
    fun theShardsAreTheSameEveryTime() {
        val a = shatterShards(size, 30)
        val b = shatterShards(size, 30)
        assertEquals(a.map { it.center }, b.map { it.center })
        assertEquals(a.map { it.spin }, b.map { it.spin })
    }

    @Test
    fun piecesCrackApartThenFall() {
        val shard = shatterShards(size, 24).first()
        val middle = Offset(100f, 50f)
        assertEquals(0f, shardOffset(shard, 0f, middle, 300f, 2f).getDistance())
        val cracked = shardOffset(shard, CRACK, middle, 300f, 2f)
        assertEquals(2f, cracked.getDistance(), 1e-3f)
        assertEquals(0f, abs(shardRotation(shard, CRACK)))
        assertTrue(shardOffset(shard, 1f, middle, 300f, 2f).y > 200f, "falls")
        assertEquals(shard.spin, shardRotation(shard, 1f))
        // A shard right on the middle has no direction to part in.
        val centered = Shard(floatArrayOf(90f, 110f, 100f), floatArrayOf(40f, 40f, 70f), 0f, 0f, 0f)
        assertEquals(0f, shardOffset(centered, CRACK, centered.center, 300f, 2f).getDistance())
    }

    @Test
    fun piecesFadeOutAtTheEnd() {
        assertEquals(1f, shatterAlpha(0f))
        assertEquals(1f, shatterAlpha(0.5f))
        assertEquals(0f, shatterAlpha(1f))
        assertTrue(shatterAlpha(0.8f) in 0f..1f)
    }
}
