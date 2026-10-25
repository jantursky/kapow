package com.jantursky.kapow

import androidx.compose.ui.unit.Density
import com.jantursky.kapow.effects.burstPeak
import com.jantursky.kapow.effects.burstScale
import com.jantursky.kapow.effects.glitchFrame
import com.jantursky.kapow.effects.spinDamping
import com.jantursky.kapow.effects.spinShade
import com.jantursky.kapow.effects.stampDustEmission
import com.jantursky.kapow.effects.stampPose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BodyOptionsMathTest {
    @Test
    fun shadingScalesTheDarkeningAndTheStandardIsUnchanged() {
        assertEquals(0f, spinShade(0f))
        assertEquals(0.35f, spinShade(90f), 1e-5f)
        assertEquals(0.7f, spinShade(90f, shading = 0.7f), 1e-5f)
        assertEquals(0f, spinShade(90f, shading = 0f))
    }

    @Test
    fun overshootSetsTheSpringsDamping() {
        assertEquals(0.65f, spinDamping(0.35f))
        assertEquals(1f, spinDamping(0f))
        assertTrue(spinDamping(0.8f) < spinDamping(0.35f))
        assertEquals(0.05f, spinDamping(0.95f), 1e-5f)
    }

    @Test
    fun aBurstPopsAsFarAsItsOvershootSays() {
        val (peak, end) = burstPeak(0.15f)
        assertEquals(1.15f, peak)
        assertEquals(1.05f, end)
        assertEquals(1.15f, burstScale(0.3f))
        assertEquals(burstScale(0.6f), burstScale(0.6f, peak, end))
        val (bigPeak, bigEnd) = burstPeak(0.45f)
        assertEquals(1.45f, burstScale(0.3f, bigPeak, bigEnd), 1e-5f)
        assertTrue(bigEnd > end)
        assertEquals(1f, burstPeak(0f).first)
    }

    @Test
    fun aStampLandsLevelAndStartsTilted() {
        assertEquals(0f, stampPose(0f, 1.3f).rotation)
        assertEquals(30f, stampPose(0f, 1.3f, tilt = 30f).rotation)
        assertEquals(0f, stampPose(STAMP_DROP_SECONDS, 1.3f, tilt = 30f).rotation)
        assertTrue(stampPose(0.07f, 1.3f, tilt = 30f).rotation in 1f..29f)
    }

    @Test
    fun squashAndReboundShapeTheLanding() {
        val samples = (0..30).map { 0.14f + it * 0.014f }
        val flat = samples.minOf { stampPose(it, 1f).squashY }
        val flatter = samples.minOf { stampPose(it, 1f, squash = 0.2f).squashY }
        assertTrue(flatter < flat - 0.05f, "a bigger squash flattens more: $flat $flatter")
        // Without rebound the element never stretches taller than it is.
        assertTrue(samples.all { stampPose(it, 1f, rebound = 0f).squashY <= 1f + 1e-6f })
        assertTrue(samples.any { stampPose(it, 1f, rebound = 1f).squashY > 1f + 1e-3f })
        assertEquals(stampPose(0.2f, 1.3f), stampPose(0.2f, 1.3f, 0.1f, 1f, 0f))
    }

    @Test
    fun dustCountAndReachAreAdjustable() {
        val density = Density(1f)
        assertEquals(stampDustEmission(0f, density), stampDustEmission(0f, density, 5, 1f))
        assertEquals(9, stampDustEmission(0f, density, 9).count)
        assertEquals(stampDustEmission(0f, density).speed.max * 2, stampDustEmission(0f, density, reach = 2f).speed.max)
    }

    @Test
    fun glitchSlicesFollowTheirSettings() {
        val default = glitchFrame(3, 4, 200f, 6f)
        assertEquals(default, glitchFrame(3, 4, 200f, 6f, 0.08f, 0.3f, 0.5f))
        val tall = glitchFrame(3, 4, 200f, 6f, sliceMin = 0.5f, sliceMax = 0.6f)
        assertTrue(tall.slices.size <= 3, "tall slices are few: ${tall.slices.size}")
        assertTrue(default.slices.size > tall.slices.size)
        val still = glitchFrame(3, 4, 200f, 6f, jumpChance = 0f)
        assertTrue(still.slices.all { it.shift == 0f })
        val jumpy = glitchFrame(3, 4, 200f, 6f, jumpChance = 1f)
        assertTrue(jumpy.slices.all { it.shift != 0f })
        assertEquals(200f, jumpy.slices.last().bottom)
        assertNotEquals(glitchFrame(3, 4, 200f, 6f), glitchFrame(4, 4, 200f, 6f))
    }

    private companion object {
        const val STAMP_DROP_SECONDS = 0.14f
    }
}
