package com.jantursky.kapow

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.bubbleEmission
import com.jantursky.kapow.effects.bubbleGrowth
import com.jantursky.kapow.effects.bubbleOpacity
import com.jantursky.kapow.particles.ParticleSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BubblesMathTest {
    @Test
    fun aBubbleAppearsStaysClearThenPops() {
        assertEquals(0f, bubbleOpacity(0f))
        assertEquals(1f, bubbleOpacity(0.5f))
        assertEquals(0f, bubbleOpacity(1f))
        assertTrue(bubbleOpacity(0.95f) in 0f..1f)
    }

    @Test
    fun aBubbleGrowsAndSwellsAsItPops() {
        assertTrue(bubbleGrowth(0.5f) > bubbleGrowth(0f))
        assertTrue(bubbleGrowth(1f) > bubbleGrowth(0.9f) + 0.3f)
    }

    @Test
    fun bubblesRise() {
        val system = ParticleSystem(bubbleEmission(100f, Density(1f)), 1)
        val start = system.y[0]
        system.step(0.5f)
        assertTrue(system.y[0] < start - 5f)
    }

    @Test
    fun theFactoryChecksItsArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.bubbles(size = 0.dp) }
    }
}
