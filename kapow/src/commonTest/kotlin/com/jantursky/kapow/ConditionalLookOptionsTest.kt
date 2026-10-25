package com.jantursky.kapow

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.beamHead
import com.jantursky.kapow.effects.beamTailAlpha
import com.jantursky.kapow.effects.glowBreath
import com.jantursky.kapow.effects.grayscaleMatrix
import com.jantursky.kapow.effects.repeatWait
import com.jantursky.kapow.effects.roundCapPattern
import com.jantursky.kapow.effects.spotlightPadding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ConditionalLookOptionsTest {
    @Test
    fun aBreathingHaloSwellsAndShrinksAroundItsSteadyStrength() {
        assertEquals(1f, glowBreath(0.3f, 0f))
        assertEquals(1f, glowBreath(0f, 0.5f))
        assertTrue(glowBreath(0.25f, 0.5f) > 1.4f)
        assertTrue(glowBreath(0.75f, 0.5f) < 0.6f)
        assertEquals(0f, glowBreath(0.75f, 1f), 1e-6f)
    }

    @Test
    fun theBeamTailFadesByTheChosenExponent() {
        assertEquals(beamTailAlpha(0.4f), beamTailAlpha(0.4f, 1.5f))
        assertTrue(beamTailAlpha(0.5f, 3f) < beamTailAlpha(0.5f))
        assertEquals(1f, beamTailAlpha(0.2f, 0f))
        assertEquals(1f, beamTailAlpha(1f, 4f))
    }

    @Test
    fun beamsAreSpacedEvenlyAndTurnBothWays() {
        assertEquals(0f, beamHead(0f, 0, 2, 100f, true))
        assertEquals(50f, beamHead(0f, 1, 2, 100f, true))
        assertEquals(25f, beamHead(0.25f, 0, 1, 100f, true))
        assertEquals(75f, beamHead(0.25f, 0, 1, 100f, false))
        assertEquals(25f, beamHead(0.75f, 1, 2, 100f, true))
        assertTrue(beamHead(0.9f, 1, 2, 100f, true) in 0f..100f)
    }

    @Test
    fun roundCapsKeepThePeriodOfThePattern() {
        val (on, off) = roundCapPattern(8f, 4f, 3f)
        assertEquals(5f, on)
        assertEquals(12f, on + off)
        val (dot, rest) = roundCapPattern(2f, 4f, 3f)
        assertTrue(dot < 0.1f)
        assertEquals(6f, dot + rest, 1e-4f)
    }

    @Test
    fun theSpotlightBreathesAroundItsPadding() {
        assertEquals(8f, spotlightPadding(8f, 0f, 0.3f))
        assertEquals(8f, spotlightPadding(8f, 4f, 0f), 1e-5f)
        assertEquals(12f, spotlightPadding(8f, 4f, 0.25f), 1e-4f)
        assertEquals(0f, spotlightPadding(2f, 6f, 0.75f))
    }

    @Test
    fun grayscaleOnlyNeedsAMatrixWhenTintingOrDimming() {
        assertNull(grayscaleMatrix(0f, null, 0f, 1f))
        val gray = assertNotNull(grayscaleMatrix(0f, Color.Red, 0f, 1f))
        // Fully tinted red: green and blue are multiplied away, red is kept.
        assertEquals(0f, gray[5])
        assertEquals(0f, gray[10])
        assertEquals(0.213f, gray[0], 1e-4f)
        assertEquals(1f, gray[18])
        val dimmed = assertNotNull(grayscaleMatrix(1f, null, 0.5f, 1f))
        assertEquals(0.5f, dimmed[0], 1e-6f)
        assertEquals(0.5f, dimmed[6], 1e-6f)
        val half = assertNotNull(grayscaleMatrix(1f, null, 0.5f, 0f))
        assertEquals(1f, half[0], 1e-6f)
    }

    @Test
    fun repeatJittersEachWaitWithinABound() {
        assertEquals(1.seconds, repeatWait(1.seconds, 0f, 0.9f, 66.milliseconds))
        assertEquals(1.5.seconds, repeatWait(1.seconds, 0.5f, 1f, 66.milliseconds))
        assertEquals(500.milliseconds, repeatWait(1.seconds, 0.5f, -1f, 66.milliseconds))
        assertEquals(66.milliseconds, repeatWait(100.milliseconds, 1f, -1f, 66.milliseconds))
    }

    @Test
    fun equalOptionsGiveEqualEffects() {
        assertEquals(
            ConditionalEffect.borderBeam(beams = 2, brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))),
            ConditionalEffect.borderBeam(beams = 2, brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))),
        )
        assertNotEquals(ConditionalEffect.borderBeam(beams = 2), ConditionalEffect.borderBeam(beams = 3))
        assertEquals(
            ConditionalEffect.rainbowBorder(easing = FastOutSlowInEasing),
            ConditionalEffect.rainbowBorder(easing = FastOutSlowInEasing),
        )
        assertEquals(ConditionalEffect.grayscale(tint = Color.Red), ConditionalEffect.grayscale(tint = Color.Red))
        assertNotEquals(ConditionalEffect.pushDown(dim = 0.1f), ConditionalEffect.pushDown())
        assertEquals(ConditionalEffect.glow(breathe = 0.4f), ConditionalEffect.glow(breathe = 0.4f))
    }

    @Test
    fun theFactoriesCheckTheNewOptions() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.glow(strength = 0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.glow(strength = 2f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.glow(breathe = 1.5f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.glow(period = 0.seconds) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.glow(innerGlow = -0.1f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.pushDown(damping = 0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.pushDown(stiffness = -1f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.pushDown(dim = 2f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.borderBeam(trackAlpha = 2f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.borderBeam(beams = 0) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.borderBeam(glow = -1f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.borderBeam(tailFade = -1f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.rainbowBorder(alpha = 0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.rainbowBorder(glow = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.grayscale(dim = 1.5f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.spotlight(ring = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.spotlight(breathe = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.spotlight(period = 0.seconds) }
        val change = ChangeEffect.flash()
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.repeat(change, 1.seconds, maxCount = 0) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.repeat(change, 1.seconds, jitter = 2f) }
        assertFailsWith<IllegalArgumentException> {
            ConditionalEffect.repeat(change, 1.seconds, initialDelay = (-1).seconds)
        }
    }
}
