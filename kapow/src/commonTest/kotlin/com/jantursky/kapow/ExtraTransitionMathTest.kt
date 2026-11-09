package com.jantursky.kapow

import com.jantursky.kapow.transitions.blurRadius
import com.jantursky.kapow.transitions.boingScale
import com.jantursky.kapow.transitions.flickerOn
import com.jantursky.kapow.transitions.flickerSteps
import com.jantursky.kapow.transitions.rotate3DAngle
import com.jantursky.kapow.transitions.skidOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ExtraTransitionMathTest {
    @Test
    fun boingGrowsFromNothingAndSettlesAtFullSize() {
        val start = boingScale(0f)
        assertEquals(0f, start.x)
        assertEquals(0f, start.y)
        val end = boingScale(1f, squash = 1f)
        assertEquals(1f, end.x, 0.0001f)
        assertEquals(1f, end.y, 0.0001f)
    }

    @Test
    fun boingPassesItsSizeByTheOvershoot() {
        val peak = (1..99).maxOf { boingScale(it / 100f, overshoot = 1f).x }
        val higher = (1..99).maxOf { boingScale(it / 100f, overshoot = 2f).x }
        val none = (1..99).maxOf { boingScale(it / 100f, overshoot = 0f).x }
        assertTrue(peak > 1.05f, "$peak")
        assertTrue(higher > peak, "$higher against $peak")
        assertEquals(1f, none, 0.0001f, "no overshoot never passes full size")
    }

    @Test
    fun squashStretchesTheOneSideAndFlattensTheOther() {
        val big = (1..99).map { boingScale(it / 100f, squash = 1f) }.maxBy { it.y }
        assertTrue(big.y > big.x, "taller than wide while big: $big")
        val plain = boingScale(0.4f, squash = 0f)
        assertEquals(plain.x, plain.y)
    }

    @Test
    fun skidTravelsFromOneSizeAwayPastItsPlaceAndSettlesThere() {
        assertEquals(-1f, skidOffset(0f), 0.0001f)
        assertEquals(0f, skidOffset(1f), 0.0001f)
        assertTrue((1..99).any { skidOffset(it / 100f) > 0.01f }, "overshoots")
        assertTrue((1..99).all { skidOffset(it / 100f, overshoot = 0f) <= 0.0001f }, "no overshoot")
    }

    @Test
    fun rotate3DTurnsFromTheStartAngleToZero() {
        assertEquals(90f, rotate3DAngle(0f, 90f))
        assertEquals(0f, rotate3DAngle(1f, 90f))
        assertEquals(45f, rotate3DAngle(0.5f, 90f))
    }

    @Test
    fun theBlurRadiusFallsToZero() {
        assertEquals(16f, blurRadius(0f, 16f))
        assertEquals(0f, blurRadius(1f, 16f))
    }

    @Test
    fun theSameSeedGivesTheSameFlicker() {
        assertEquals(flickerSteps(7, 8, 800.milliseconds), flickerSteps(7, 8, 800.milliseconds))
        assertNotEquals(flickerSteps(7, 8, 1200.milliseconds), flickerSteps(8, 8, 1200.milliseconds))
    }

    @Test
    fun aFlickerEndsOnAfterStartingOff() {
        val ends = flickerSteps(61, 8, 2.seconds)
        assertEquals(1f, ends.last())
        assertTrue(ends.zipWithNext().all { (a, b) -> b > a })
        assertTrue(!flickerOn(ends, 0.0001f), "starts off")
        assertTrue(flickerOn(ends, 0.9999f), "ends on")
        assertTrue(flickerOn(ends, 1f))
    }

    @Test
    fun noSecondHoldsMoreThanThreeFlashes() {
        for (duration in listOf(300.milliseconds, 800.milliseconds, 1.seconds, 2.seconds, 5.seconds)) {
            for (steps in listOf(2, 8, 30)) {
                val seconds = duration.inWholeMilliseconds / 1000f
                val ends = flickerSteps(61, steps, duration)
                val lengths = ends.mapIndexed { i, end -> (end - (if (i == 0) 0f else ends[i - 1])) * seconds }
                assertTrue(
                    lengths.size == 1 || lengths.all { it >= 1f / 6f - 0.001f },
                    "$duration with $steps steps: $lengths",
                )
            }
        }
    }

    @Test
    fun theShortestTransitionIsOneOnStep() {
        assertEquals(listOf(1f), flickerSteps(61, 30, 200.milliseconds))
        assertTrue(flickerOn(flickerSteps(61, 30, 200.milliseconds), 0.5f))
    }

    @Test
    fun theOptionsAreChecked() {
        assertFailsWith<IllegalArgumentException> { KapowEnter.rotate3D(axis = SpinAxis.Z) }
        assertFailsWith<IllegalArgumentException> { KapowExit.rotate3D(axis = SpinAxis.Z) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.boing(squash = 2f) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.flicker(steps = 1) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.flicker(dim = 2f) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.skid(lean = 90f) }
        assertFailsWith<IllegalArgumentException> { KapowExit.blur(scale = 0f) }
        assertFailsWith<IllegalArgumentException> { KapowExit.blur(duration = 0.milliseconds) }
    }
}
