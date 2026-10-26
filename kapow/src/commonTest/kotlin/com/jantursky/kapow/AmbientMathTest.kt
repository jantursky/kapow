package com.jantursky.kapow

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.breatheScale
import com.jantursky.kapow.effects.breatheWave
import com.jantursky.kapow.effects.pingAlpha
import com.jantursky.kapow.effects.pingPhase
import com.jantursky.kapow.effects.pingScale
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AmbientMathTest {
    private fun near(
        expected: Float,
        actual: Float,
    ) = assertTrue(abs(expected - actual) < 1e-4f, "expected $expected but was $actual")

    @Test
    fun aBreathRestsGrowsAndReturns() {
        near(0f, breatheWave(0.0, 3.0))
        near(0.5f, breatheWave(0.75, 3.0))
        near(1f, breatheWave(1.5, 3.0))
        near(0f, breatheWave(3.0, 3.0))
        near(0f, breatheWave(6.0, 3.0))
    }

    @Test
    fun theScaleFollowsTheWaveAndHowFarTheEffectIsIn() {
        near(1f, breatheScale(0f, 1f, 1.2f))
        near(1.2f, breatheScale(1f, 1f, 1.2f))
        near(1.1f, breatheScale(1f, 0.5f, 1.2f))
        near(1f, breatheScale(1f, 0f, 1.2f))
    }

    @Test
    fun ringsAreBornEvenlyAndOnlyWhileOn() {
        assertEquals(0f, pingPhase(0.0, 1.6, 2, 0))
        assertNull(pingPhase(0.5, 1.6, 2, 1), "the second ring waits for its turn")
        assertEquals(0f, pingPhase(0.8, 1.6, 2, 1))
        near(0.5f, pingPhase(0.8, 1.6, 2, 0)!!)
        near(0.5f, pingPhase(1.6, 1.6, 2, 1)!!)
        // The first ring is reborn at 1.6 seconds.
        near(0.25f, pingPhase(2.0, 1.6, 2, 0)!!)
    }

    @Test
    fun ringsInFlightFinishAndNoNewOneStarts() {
        // Switched off at 1.0 seconds.
        near(0.9375f, pingPhase(1.5, 1.6, 2, 0, offAt = 1.0)!!)
        assertNull(pingPhase(1.7, 1.6, 2, 0, offAt = 1.0), "the ring that would be reborn at 1.6 never starts")
        assertNotNull(pingPhase(2.0, 1.6, 2, 1, offAt = 1.0), "the ring born at 0.8 is still in flight")
        assertNull(pingPhase(2.5, 1.6, 2, 1, offAt = 1.0))
    }

    @Test
    fun ringsStartAgainWhenSwitchedBackOn() {
        // Off at 1.0, on again at 2.0: the ring that would have been reborn at 1.6 is skipped, the one at 2.4 starts.
        assertNull(pingPhase(2.0, 1.6, 2, 0, offAt = 1.0, onAt = 2.0))
        assertNotNull(pingPhase(2.5, 1.6, 2, 1, offAt = 1.0, onAt = 2.0))
    }

    @Test
    fun aRingGrowsToItsScaleAndFadesToNothing() {
        near(1f, pingScale(0f, 1.6f))
        near(1.6f, pingScale(1f, 1.6f))
        assertTrue(pingScale(0.5f, 1.6f) > 1.3f, "fast at first")
        near(1f, pingAlpha(0f))
        near(0f, pingAlpha(1f))
        assertTrue(pingAlpha(0.3f) > pingAlpha(0.6f))
    }

    @Test
    fun theFactoriesCheckTheirArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.breathe(scale = 0.9f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.breathe(float = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.breathe(duration = Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.ping(rings = 0) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.ping(rings = 9) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.ping(scale = 1f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.ping(duration = 0.seconds) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.ping(width = 0.dp) }
        ConditionalEffect.ping(shape = RectangleShape)
    }
}
