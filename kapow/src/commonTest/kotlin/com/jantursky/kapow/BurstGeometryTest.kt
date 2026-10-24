package com.jantursky.kapow

import androidx.compose.ui.geometry.Size
import com.jantursky.kapow.effects.burstOpacity
import com.jantursky.kapow.effects.burstOutline
import com.jantursky.kapow.effects.burstRadius
import com.jantursky.kapow.effects.burstScale
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BurstGeometryTest {
    @Test
    fun tipsAndNotchesAlternateInsideTheUnitCircle() {
        val points = burstOutline(spikes = 9, seed = 4)
        assertEquals(36, points.size)
        for (k in 0 until 18) {
            val r = hypot(points[2 * k], points[2 * k + 1])
            if (k % 2 == 0) {
                assertTrue(r in 0.82f..1f, "tip $k at $r")
            } else {
                assertTrue(r in 0.5f..0.62f, "notch $k at $r")
            }
        }
        assertTrue(points[1] < -0.8f, "the first tip points up")
    }

    @Test
    fun theSameSeedGivesTheSameOutline() {
        assertContentEquals(burstOutline(12, 3), burstOutline(12, 3))
        assertFalse(burstOutline(12, 3).contentEquals(burstOutline(12, 4)))
    }

    @Test
    fun theBurstReachesPastTheCorners() {
        assertEquals(50f + 30f, burstRadius(Size(80f, 60f), maxExtraPx = 100f))
        assertEquals(50f + 10f, burstRadius(Size(80f, 60f), maxExtraPx = 10f))
    }

    @Test
    fun itPopsPastFullSizeThenSettlesAndFades() {
        assertEquals(0.3f, burstScale(0f))
        assertEquals(1.15f, burstScale(0.3f), 0.0001f)
        assertEquals(1.05f, burstScale(1f), 0.0001f)
        assertTrue(burstScale(0.1f) > 0.6f, "most of the pop is early")
        assertEquals(1f, burstOpacity(0.5f))
        assertEquals(0.5f, burstOpacity(0.75f))
        assertEquals(0f, burstOpacity(1f))
    }
}
