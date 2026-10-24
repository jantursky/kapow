package com.jantursky.kapow

import androidx.compose.ui.geometry.Size
import com.jantursky.kapow.effects.sparklePlacements
import com.jantursky.kapow.effects.sparkleSize
import com.jantursky.kapow.effects.twinkle
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SparkleMathTest {
    private val size = Size(200f, 100f)

    @Test
    fun starsStayOnTheElement() {
        val points = sparklePlacements(9, size, seed = 1)
        assertEquals(18, points.size)
        for (i in 0 until 9) {
            assertTrue(points[2 * i] in 0f..200f, "x of star $i")
            assertTrue(points[2 * i + 1] in 0f..100f, "y of star $i")
        }
    }

    @Test
    fun starsDoNotClump() {
        // Eight stars on a 2:1 element take eight different cells of a 4 × 2 grid.
        val points = sparklePlacements(8, size, seed = 7)
        val cells = (0 until 8).map { (points[2 * it] / 50).toInt() to (points[2 * it + 1] / 50).toInt() }.toSet()
        assertEquals(8, cells.size)
    }

    @Test
    fun theSameSeedGivesTheSameStars() {
        assertContentEquals(sparklePlacements(5, size, 3), sparklePlacements(5, size, 3))
        assertEquals(2, sparklePlacements(1, Size(0f, 0f), 3).size)
    }

    @Test
    fun starSizeFollowsTheElementWithinLimits() {
        assertEquals(20f, sparkleSize(Size(60f, 90f), 10f, 30f))
        assertEquals(10f, sparkleSize(Size(12f, 90f), 10f, 30f))
        assertEquals(30f, sparkleSize(size, 10f, 30f))
    }

    @Test
    fun aStarSwellsAndDiesAway() {
        assertEquals(0f, twinkle(0f))
        assertEquals(1f, twinkle(0.5f))
        assertEquals(0f, twinkle(1f), 0.0001f)
        assertEquals(twinkle(0.2f), twinkle(0.8f), 0.0001f)
    }
}
