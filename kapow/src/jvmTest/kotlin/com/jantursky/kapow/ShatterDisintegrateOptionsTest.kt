package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.transitions.CRACK
import com.jantursky.kapow.transitions.DUST_SWEEP
import com.jantursky.kapow.transitions.SHATTER_SEED
import com.jantursky.kapow.transitions.cellGrid
import com.jantursky.kapow.transitions.dustHidden
import com.jantursky.kapow.transitions.dustMoment
import com.jantursky.kapow.transitions.shardOffset
import com.jantursky.kapow.transitions.shardRotation
import com.jantursky.kapow.transitions.shatterShards
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ShatterDisintegrateOptionsTest {
    private val size = Size(200f, 100f)
    private val middle = Offset(100f, 50f)

    @Test
    fun theDefaultSeedAndImpactAreTheOldPattern() {
        val old = shatterShards(size, 24)
        val same = shatterShards(size, 24, SHATTER_SEED, 100f)
        assertEquals(old.size, same.size)
        for ((a, b) in old.zip(same)) {
            assertEquals(a.driftX, b.driftX)
            assertEquals(a.kick, b.kick)
            assertEquals(a.spin, b.spin)
            assertEquals(a.center, b.center)
        }
    }

    @Test
    fun anotherSeedMakesAnotherPattern() {
        val a = shatterShards(size, 24, seed = 1)
        val b = shatterShards(size, 24, seed = 2)
        val again = shatterShards(size, 24, seed = 1)
        assertEquals(a.map { it.center }, again.map { it.center }, "the same seed, the same pieces")
        assertTrue(a.size != b.size || a.zip(b).any { (x, y) -> x.center != y.center || x.spin != y.spin })
    }

    @Test
    fun theImpactSetsWhichWayThePiecesLean() {
        val atLeft = shatterShards(size, 24, impactX = 0f)
        val atRight = shatterShards(size, 24, impactX = 200f)
        assertTrue(atLeft.all { it.driftX >= -0.08f }, "struck on the left, they lean right")
        assertTrue(atRight.all { it.driftX <= 0.08f }, "struck on the right, they lean left")
        assertTrue(atLeft.sumOf { it.driftX.toDouble() } > atRight.sumOf { it.driftX.toDouble() })
    }

    @Test
    fun gravityScalesTheFall() {
        val shard = shatterShards(size, 24).first()
        val usual = shardOffset(shard, 1f, middle, 300f, 2f)
        assertEquals(usual, shardOffset(shard, 1f, middle, 300f, 2f, gravity = 1f))
        assertTrue(shardOffset(shard, 1f, middle, 300f, 2f, gravity = 2f).y > usual.y, "faster")
        assertTrue(shardOffset(shard, 1f, middle, 300f, 2f, gravity = 0f).y < 0f, "no gravity, only the throw")
        assertEquals(
            shardOffset(shard, CRACK, middle, 300f, 2f),
            shardOffset(shard, CRACK, middle, 300f, 2f, gravity = 3f),
            "the cracks do not depend on it",
        )
    }

    @Test
    fun spinScalesTheTurn() {
        val shard = shatterShards(size, 24).first { it.spin != 0f }
        assertEquals(shard.spin, shardRotation(shard, 1f))
        assertEquals(shard.spin * 2, shardRotation(shard, 1f, spin = 2f))
        assertEquals(0f, abs(shardRotation(shard, 1f, spin = 0f)))
    }

    @Test
    fun theDustSeedChangesWhichGrainsGoFirst() {
        val moments = (0 until 20).map { dustMoment(5, it, 40) }
        assertEquals(moments, (0 until 20).map { dustMoment(5, it, 40, seed = 71) })
        assertNotEquals(moments, (0 until 20).map { dustMoment(5, it, 40, seed = 5) })
    }

    @Test
    fun theGrainSizeSetsTheGrainOfTheEdge() {
        val area = Size(120f, 60f)
        assertEquals(IntSize(40, 20), cellGrid(area, 3f))
        assertEquals(IntSize(10, 5), cellGrid(area, 12f))
        // Whatever the grain, the band has swept the whole content by the end of the sweep.
        for (cell in listOf(3f, 12f)) {
            assertEquals(area, dustHidden(area, cell, DUST_SWEEP).getBounds().size)
        }
    }

    @Test
    fun theOptionsAreChecked() {
        assertFailsWith<IllegalArgumentException> { KapowExit.shatter(gravity = -1f) }
        assertFailsWith<IllegalArgumentException> { KapowExit.shatter(spin = -0.1f) }
        assertFailsWith<IllegalArgumentException> { KapowExit.disintegrate(particleSize = 0.dp) }
        assertFailsWith<IllegalArgumentException> { KapowExit.disintegrate(particleSize = 17.dp) }
        assertFailsWith<IllegalArgumentException> { KapowExit.disintegrate(wind = 1.1f) }
        assertFailsWith<IllegalArgumentException> { KapowExit.disintegrate(wind = -1.1f) }
        KapowExit.shatter(gravity = 0f, spin = 0f, impact = TransformOrigin(0f, 1f), seed = -4)
        KapowExit.disintegrate(particleSize = 1.dp, wind = -1f)
        KapowExit.disintegrate(particleSize = 16.dp, wind = 1f)
    }

    @Test
    fun equalOptionsMakeEqualTransitions() {
        assertEquals(KapowExit.shatter(), KapowExit.shatter())
        assertEquals(KapowExit.shatter(gravity = 2f, seed = 3), KapowExit.shatter(gravity = 2f, seed = 3))
        assertNotEquals(KapowExit.shatter(), KapowExit.shatter(gravity = 2f))
        assertNotEquals(KapowExit.shatter(), KapowExit.shatter(spin = 0f))
        assertNotEquals(KapowExit.shatter(), KapowExit.shatter(impact = TransformOrigin(0f, 0f)))
        assertNotEquals(KapowExit.shatter(), KapowExit.shatter(seed = 1))
        assertNotEquals(KapowExit.disintegrate(), KapowExit.disintegrate(particleSize = 6.dp))
        assertNotEquals(KapowExit.disintegrate(), KapowExit.disintegrate(wind = 0.5f))
        assertNotEquals(KapowExit.disintegrate(), KapowExit.disintegrate(color = Color.Red))
    }
}
