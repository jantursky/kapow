package com.jantursky.kapow

import com.jantursky.kapow.effects.jumpHeight
import com.jantursky.kapow.effects.jumpSquash
import com.jantursky.kapow.effects.shakeOffset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ShakeJumpMathTest {
    @Test
    fun theShakeStartsAndEndsAtRest() {
        assertEquals(0f, shakeOffset(0f, 4, 1f))
        assertEquals(0f, shakeOffset(1f, 4, 1f))
        assertEquals(0f, shakeOffset(2f, 4, 1f), "past the end stays at rest")
    }

    @Test
    fun theSwingsShrink() {
        val peaks =
            (0 until 4).map { swing ->
                // The peak of a swing is a quarter of the way through it.
                abs(shakeOffset((swing + 0.25f) / 4f, 4, 1f))
            }
        assertTrue(peaks.zipWithNext().all { (a, b) -> b < a }, "$peaks")
        assertTrue(peaks.first() > 0.5f)
    }

    @Test
    fun theShakeSwingsBothWays() {
        val samples = (1 until 100).map { shakeOffset(it / 100f, 3, 1f) }
        assertTrue(samples.any { it > 0.1f } && samples.any { it < -0.1f })
    }

    @Test
    fun aHigherDecayShrinksTheSwingsSooner() {
        val slow = abs(shakeOffset(0.625f, 4, 1f))
        val fast = abs(shakeOffset(0.625f, 4, 3f))
        assertTrue(fast < slow, "$fast against $slow")
    }

    @Test
    fun aHopRisesToTheTopAndLandsOnTheGround() {
        assertEquals(0f, jumpHeight(0f))
        assertEquals(1f, jumpHeight(0.4f), 0.001f)
        assertEquals(0f, jumpHeight(0.8f), 0.001f)
        assertEquals(0f, jumpHeight(1f))
    }

    @Test
    fun twoHopsReachTheTopTwice() {
        assertEquals(1f, jumpHeight(0.2f, hops = 2), 0.001f)
        assertEquals(1f, jumpHeight(0.7f, hops = 2), 0.001f)
        assertEquals(0f, jumpHeight(0.5f, hops = 2), 0.001f, "on the ground between the hops")
        assertEquals(0f, jumpHeight(1f, hops = 2))
    }

    @Test
    fun theSquashBuildsOnLandingAndRecovers() {
        assertEquals(0f, jumpSquash(0f))
        assertEquals(0f, jumpSquash(0.4f), "none in the air")
        assertEquals(1f, jumpSquash(0.9f), 0.001f, "most flattened mid-way through the landing")
        assertEquals(0f, jumpSquash(1f), "recovered at the end")
        assertEquals(0f, jumpSquash(0.4999f, hops = 2), 0.01f, "the first hop recovers before the second")
    }

    @Test
    fun theOptionsAreChecked() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.shake(swings = 0) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.shake(swings = 21) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.shake(decay = -1f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.jump(hops = 0) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.jump(squash = 1.5f) }
    }
}
