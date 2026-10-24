package com.jantursky.kapow

import com.jantursky.kapow.effects.rippleOpacity
import com.jantursky.kapow.effects.rippleRadius
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RippleMathTest {
    @Test
    fun theRadiusGrowsFromNothingToTheFarthestCorner() {
        assertEquals(0f, rippleRadius(0f, 100f))
        assertEquals(100f, rippleRadius(1f, 100f))
        assertEquals(100f, rippleRadius(2f, 100f))
    }

    @Test
    fun theRadiusSlowsDown() {
        assertTrue(rippleRadius(0.25f, 100f) > 50f, "more than half the way after a quarter of the time")
        val early = rippleRadius(0.2f, 100f) - rippleRadius(0.1f, 100f)
        val late = rippleRadius(0.9f, 100f) - rippleRadius(0.8f, 100f)
        assertTrue(early > late)
    }

    @Test
    fun theInkHoldsThenFades() {
        assertEquals(1f, rippleOpacity(0f))
        assertEquals(1f, rippleOpacity(0.4f))
        assertEquals(0.5f, rippleOpacity(0.7f), 0.0001f)
        assertEquals(0f, rippleOpacity(1f))
        assertEquals(1f, rippleOpacity(-1f))
    }
}
