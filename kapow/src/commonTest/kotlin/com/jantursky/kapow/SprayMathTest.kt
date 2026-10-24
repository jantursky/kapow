package com.jantursky.kapow

import androidx.compose.ui.unit.Density
import com.jantursky.kapow.effects.fadeOutLate
import com.jantursky.kapow.effects.growIn
import com.jantursky.kapow.effects.riseEmission
import com.jantursky.kapow.effects.sprayEmission
import com.jantursky.kapow.particles.ParticleSystem
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SprayMathTest {
    private val density = Density(2f)

    @Test
    fun sprayFansUpwardAndIsGoneWithinAboutASecond() {
        val system = ParticleSystem(sprayEmission(12, density), seed = 3)
        repeat(12) { system.step(1f / 60) }
        for (i in 0 until system.count) assertTrue(system.y[i] < 0f, "particle $i rose")
        repeat(54) { system.step(1f / 60) }
        assertFalse(system.step(1f / 60), "all gone after 1.1 seconds")
    }

    @Test
    fun riseFloatsUpAndSwaysWithinItsWobble() {
        val emission = riseEmission(density)
        val system = ParticleSystem(emission, seed = 5)
        var minY = 0f
        repeat(60) {
            system.step(1f / 60)
            minY = minOf(minY, system.y[0])
            assertTrue(abs(system.drawnX(0)) <= emission.wobble + 0.001f)
        }
        assertTrue(minY < -40f, "rose by $minY px")
        assertTrue(system.isAlive(0))
    }

    @Test
    fun particlesGrowInThenFadeLate() {
        assertEquals(0.4f, growIn(0f))
        assertEquals(1f, growIn(0.15f))
        assertEquals(1f, growIn(0.8f))
        assertEquals(1f, fadeOutLate(0.4f))
        assertEquals(0.5f, fadeOutLate(0.75f))
        assertEquals(0f, fadeOutLate(1f))
    }
}
