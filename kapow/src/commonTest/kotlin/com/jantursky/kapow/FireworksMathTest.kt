package com.jantursky.kapow

import androidx.compose.ui.unit.Density
import com.jantursky.kapow.effects.RISE_SECONDS
import com.jantursky.kapow.effects.emberEmission
import com.jantursky.kapow.effects.explosionEmission
import com.jantursky.kapow.effects.rocketEmission
import com.jantursky.kapow.effects.rocketRise
import com.jantursky.kapow.effects.sparkGrowth
import com.jantursky.kapow.effects.sparkOpacity
import com.jantursky.kapow.particles.ParticleSystem
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FireworksMathTest {
    private val density = Density(2f)

    @Test
    fun theRocketStopsAtItsHeightJustAsItBursts() {
        val system = ParticleSystem(rocketEmission(height = 200f), seed = 1)
        val frames = (RISE_SECONDS * 120).toInt()
        repeat(frames - 1) { system.step(1f / 120) }
        assertTrue(abs(system.y[0] + 200f) < 6f, "the head is near the top: ${system.y[0]}")
        assertEquals(0f, system.x[0], 0.01f)
        repeat(30) { system.step(1f / 120) }
        assertFalse(system.isAlive(0), "the rocket is gone once it bursts")
    }

    @Test
    fun theTrailFollowsTheRocketsClimb() {
        assertEquals(0f, rocketRise(0f))
        assertEquals(0.75f, rocketRise(0.5f))
        assertEquals(1f, rocketRise(1f))
        // The same path the particle system takes, up to its stepping error.
        val system = ParticleSystem(rocketEmission(height = 200f), seed = 1)
        repeat(27) { system.step(1f / 120) }
        assertTrue(abs(system.y[0] + 200f * rocketRise(27f / 120 / RISE_SECONDS)) < 6f)
    }

    @Test
    fun embersDropAwayBehindTheRocket() {
        val system = ParticleSystem(emberEmission(density), seed = 2)
        repeat(12) { system.step(1f / 60) }
        for (i in 0 until system.count) assertTrue(system.y[i] > 0f, "ember $i fell")
        repeat(15) { system.step(1f / 60) }
        assertFalse(system.step(1f / 60), "gone within 0.4 seconds")
    }

    @Test
    fun sparksSpreadInARingThenFall() {
        val system = ParticleSystem(explosionEmission(24, density), seed = 3)
        repeat(18) { system.step(1f / 60) }
        // Gravity moves every spark by the same amount, so the ring stays a ring around a center that sinks.
        val centerX = (0 until 24).map { system.x[it] }.average().toFloat()
        val centerY = (0 until 24).map { system.y[it] }.average().toFloat()
        val distances = (0 until 24).map { hypot(system.x[it] - centerX, system.y[it] - centerY) }
        assertTrue(distances.min() > distances.max() * 0.7f, "about the same distance out: $distances")
        val heightEarly = (0 until 24).map { system.y[it] }.average()
        repeat(36) { system.step(1f / 60) }
        val heightLate = (0 until 24).map { system.y[it] }.average()
        assertTrue(heightLate > heightEarly, "gravity pulls the ring down")
        repeat(30) { system.step(1f / 60) }
        assertFalse(system.step(1f / 60), "gone within 1.2 seconds")
    }

    @Test
    fun sparksBurnBrightThenFadeAndShrink() {
        assertEquals(1f, sparkOpacity(0.3f))
        assertEquals(0.5f, sparkOpacity(0.75f))
        assertEquals(0f, sparkOpacity(1f))
        assertEquals(1f, sparkGrowth(0f))
        assertEquals(0.6f, sparkGrowth(1f))
    }
}
