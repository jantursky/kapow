package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.flameColor
import com.jantursky.kapow.effects.flameEmission
import com.jantursky.kapow.effects.flameGrowth
import com.jantursky.kapow.effects.flameLife
import com.jantursky.kapow.effects.flameOpacity
import com.jantursky.kapow.particles.ParticleSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FlameMathTest {
    private val colors = listOf(Color.Yellow, Color.Red, Color.Transparent)

    @Test
    fun aPuffChangesColorAlongTheList() {
        assertEquals(Color.Yellow, flameColor(colors, 0f))
        assertEquals(Color.Red, flameColor(colors, 0.5f))
        assertEquals(Color.Transparent, flameColor(colors, 1f))
        val between = flameColor(colors, 0.25f)
        assertTrue(between.green in 0.3f..0.7f, "between yellow and red: $between")
    }

    @Test
    fun aPuffFlaresThenBurnsOut() {
        assertEquals(0f, flameOpacity(0f))
        assertTrue(flameOpacity(0.1f) > 0.9f)
        assertEquals(0f, flameOpacity(1f))
        assertTrue(flameOpacity(0.8f) < flameOpacity(0.3f))
    }

    @Test
    fun theLifeLeftFallsAndPuffsShrink() {
        assertEquals(1f, flameLife(0f))
        assertEquals(0f, flameLife(1f))
        assertTrue(flameGrowth(1f) < flameGrowth(0f))
    }

    @Test
    fun puffsRise() {
        val system = ParticleSystem(flameEmission(100f, 40f), 3)
        val start = system.y[0]
        system.step(0.3f)
        assertTrue(system.y[0] < start - 10f)
    }

    @Test
    fun theFactoryChecksItsArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.flame(listOf(Color.Red)) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.flame(height = 0.dp) }
        assertEquals(4, ConditionalEffect.FlameColors.size)
    }
}
