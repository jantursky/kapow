package com.jantursky.kapow

import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.sparkleEmission
import com.jantursky.kapow.effects.sparkleInterval
import com.jantursky.kapow.effects.twinkle
import com.jantursky.kapow.effects.twinkleOpacity
import com.jantursky.kapow.particles.ParticleSystem
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConditionalSparklesMathTest {
    @Test
    fun aSparkleTwinklesInAndOut() {
        assertEquals(0f, twinkle(0f))
        assertTrue(abs(twinkle(0.5f) - 1f) < 1e-6f)
        assertTrue(twinkle(1f) < 1e-6f)
        assertTrue(twinkleOpacity(0.1f) > twinkle(0.1f), "bright while still small")
    }

    @Test
    fun moreSparklesStartMoreOften() {
        assertTrue(sparkleInterval(8) < sparkleInterval(2))
        assertEquals(sparkleInterval(1), sparkleInterval(0))
    }

    @Test
    fun sparklesStayNearTheElementAndDoNotMove() {
        repeat(50) { seed ->
            val system = ParticleSystem(sparkleEmission(100f, 40f, 20f), seed)
            assertTrue(abs(system.x[0]) <= 60f && abs(system.y[0]) <= 30f)
            val x = system.x[0]
            system.step(0.5f)
            assertEquals(x, system.x[0])
        }
    }

    @Test
    fun theFactoryChecksItsArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.sparkles(emptyList()) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.sparkles(size = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.sparkles(count = 0) }
    }
}
