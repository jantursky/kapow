package com.jantursky.kapow

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.jantursky.kapow.effects.pulseBounds
import com.jantursky.kapow.effects.pulseReach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PulseGeometryTest {
    private val size = Size(80f, 40f)

    @Test
    fun aPulseStartsOnTheElementsBounds() {
        assertEquals(Rect(0f, 0f, 80f, 40f), pulseBounds(size, 0f, 30f))
    }

    @Test
    fun aPulseEndsGrownByItsReachOnEverySide() {
        assertEquals(Rect(-30f, -30f, 110f, 70f), pulseBounds(size, 1f, 30f))
    }

    @Test
    fun growthSlowsDown() {
        val half = -pulseBounds(size, 0.5f, 30f).left
        assertTrue(half > 15f, "more than half the reach at half time: $half")
    }

    @Test
    fun reachFollowsTheShorterSideUpToACap() {
        assertEquals(30f, pulseReach(size, maxPx = 64f))
        assertEquals(64f, pulseReach(Size(400f, 200f), maxPx = 64f))
    }
}
