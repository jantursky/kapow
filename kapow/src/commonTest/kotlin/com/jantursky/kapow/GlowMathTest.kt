package com.jantursky.kapow

import com.jantursky.kapow.effects.MAX_HALO
import com.jantursky.kapow.effects.glowImpulse
import com.jantursky.kapow.effects.haloOpacity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlowMathTest {
    @Test
    fun aFreshGlowGetsAFullImpulse() {
        assertEquals(5f, glowImpulse(running = false, velocity = -2f))
    }

    @Test
    fun repeatsAddUpToALimit() {
        assertEquals(3.5f, glowImpulse(running = true, velocity = 2f))
        assertEquals(5f, glowImpulse(running = true, velocity = 4.5f))
    }

    @Test
    fun opacityGrowsWithStrengthAndStopsAtTheCap() {
        assertEquals(0f, haloOpacity(0f))
        assertTrue(haloOpacity(0.5f) < haloOpacity(1f))
        assertEquals(haloOpacity(MAX_HALO), haloOpacity(10f))
    }
}
