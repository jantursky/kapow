package com.jantursky.kapow

import androidx.compose.ui.geometry.Size
import com.jantursky.kapow.effects.edgeDistance
import com.jantursky.kapow.effects.speedLineReach
import com.jantursky.kapow.effects.speedLineSpan
import com.jantursky.kapow.effects.speedLines
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeedLinesMathTest {
    private val size = Size(80f, 40f)

    @Test
    fun theEdgeIsMetOnTheSidesTopAndCorners() {
        assertEquals(40f, edgeDistance(size, 0f), 0.001f)
        assertEquals(20f, edgeDistance(size, PI.toFloat() / 2), 0.001f)
        assertEquals(40f, edgeDistance(size, PI.toFloat()), 0.001f)
        assertEquals(20f * sqrt(2f), edgeDistance(size, PI.toFloat() / 4), 0.001f)
    }

    @Test
    fun linesGoAllTheWayAroundWithoutBunching() {
        val lines = speedLines(count = 8, seed = 2)
        assertEquals(24, lines.size)
        val step = 2 * PI.toFloat() / 8
        for (i in 1 until 8) {
            val gap = lines[3 * i] - lines[3 * (i - 1)]
            assertTrue(gap in step * 0.4f..step * 1.6f, "gap $i is $gap")
        }
        for (i in 0 until 8) {
            assertTrue(lines[3 * i + 1] in 0.55f..1f)
            assertTrue(lines[3 * i + 2] in 0.6f..1f)
        }
        assertContentEquals(lines, speedLines(count = 8, seed = 2))
    }

    @Test
    fun theOuterEndLeadsAndTheInnerEndCatchesUp() {
        assertEquals(0f to 0f, speedLineSpan(0f))
        val (inner, outer) = speedLineSpan(0.3f)
        assertTrue(outer > 0.7f && inner < 0.1f, "a long line early: $inner..$outer")
        assertEquals(1f to 1f, speedLineSpan(1f))
        assertEquals(1f, speedLineSpan(0.55f).second)
    }

    @Test
    fun theReachFollowsTheShorterSideWithinLimits() {
        assertEquals(20f, speedLineReach(size, 10f, 50f))
        assertEquals(30f, speedLineReach(size, 30f, 50f))
        assertEquals(12f, speedLineReach(size, 5f, 12f))
    }
}
