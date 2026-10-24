package com.jantursky.kapow

import androidx.compose.ui.unit.Density
import com.jantursky.kapow.effects.STAMP_DROP
import com.jantursky.kapow.effects.STAMP_DURATION
import com.jantursky.kapow.effects.stampDustEmission
import com.jantursky.kapow.effects.stampPose
import com.jantursky.kapow.particles.ParticleSystem
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StampMathTest {
    @Test
    fun theStampComesDownFromItsStartScaleFasterAndFaster() {
        val start = stampPose(0f, 1.4f)
        assertEquals(1.4f, start.scale, 0.001f)
        assertTrue(start.alpha < 0.5f, "it starts faint")
        val early = 1.4f - stampPose(STAMP_DROP / 4, 1.4f).scale
        val late = stampPose(STAMP_DROP * 3 / 4, 1.4f).scale - stampPose(STAMP_DROP, 1.4f).scale
        assertTrue(late > early, "it accelerates: $early then $late")
    }

    @Test
    fun theImpactSquashesItFlatAndWide() {
        val impact = stampPose(STAMP_DROP, 1.4f)
        assertEquals(1f, impact.scale)
        assertEquals(1f, impact.alpha)
        assertTrue(impact.squashY < 0.95f, "flattened: ${impact.squashY}")
        assertTrue(impact.squashX > 1.05f, "widened: ${impact.squashX}")
    }

    @Test
    fun itSettlesWithoutShakingAndEndsAtRest() {
        val rebound = stampPose(STAMP_DROP + 0.13f, 1.3f).squashY
        assertTrue(rebound > 1f && rebound < 1.03f, "one small rebound: $rebound")
        for (i in 25..40) {
            val squash = stampPose(STAMP_DROP + i * 0.01f, 1.3f).squashY
            assertTrue(abs(squash - 1f) < 0.006f, "no shake after the rebound: $squash")
        }
        val end = stampPose(STAMP_DURATION, 1.3f)
        assertEquals(1f, end.scale)
        assertEquals(1f, end.squashX)
        assertEquals(1f, end.squashY)
        assertTrue(abs(stampPose(STAMP_DURATION - 0.01f, 1.3f).squashY - 1f) < 0.01f, "no jump at the end")
    }

    @Test
    fun dustRunsOutSidewaysAndClearsQuickly() {
        val system = ParticleSystem(stampDustEmission(0f, Density(2f)), seed = 2)
        repeat(10) { system.step(1f / 60) }
        for (i in 0 until system.count) assertTrue(system.x[i] > 0f, "puff $i runs to the right")
        repeat(40) { system.step(1f / 60) }
        assertFalse(system.step(1f / 60), "gone within a second")
    }
}
