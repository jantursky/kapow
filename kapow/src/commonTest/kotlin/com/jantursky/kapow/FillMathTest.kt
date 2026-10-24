package com.jantursky.kapow

import com.jantursky.kapow.effects.fillAmplitude
import com.jantursky.kapow.effects.fillSurface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FillMathTest {
    @Test
    fun theWaveIsFlatWhenEmptyAndWhenFull() {
        assertEquals(0f, fillAmplitude(0f, 8f))
        assertEquals(0f, fillAmplitude(1f, 8f), 0.0001f)
        assertEquals(8f, fillAmplitude(0.5f, 8f))
        assertEquals(4f, fillAmplitude(0.04f, 8f), 0.001f)
    }

    @Test
    fun theSurfaceRisesFromTheBottomToTheTop() {
        for (x in listOf(0f, 50f, 100f)) {
            assertEquals(100f, fillSurface(x, 100f, 100f, 0f, 8f))
            assertEquals(0f, fillSurface(x, 100f, 100f, 1f, 8f), 0.001f)
        }
        val half = (0..100).map { fillSurface(it.toFloat(), 100f, 100f, 0.5f, 8f) }
        assertTrue(half.all { it in 42f..58f })
        assertTrue(half.max() - half.min() > 10f, "the surface waves")
    }

    @Test
    fun theWaveRollsAsTheProgressChanges() {
        // Measured from the level, the surface above one point moves as the progress changes.
        assertNotEquals(fillSurface(30f, 100f, 100f, 0.5f, 8f) - 50f, fillSurface(30f, 100f, 100f, 0.55f, 8f) - 45f)
    }

    @Test
    fun aPhaseShiftsTheWave() {
        assertNotEquals(fillSurface(30f, 100f, 100f, 0.5f, 8f), fillSurface(30f, 100f, 100f, 0.5f, 8f, 3.14f))
    }
}
