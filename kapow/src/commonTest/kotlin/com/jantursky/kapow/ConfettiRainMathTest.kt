package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.MAX_RAIN_PIECES
import com.jantursky.kapow.effects.rainBudget
import com.jantursky.kapow.effects.rainEmission
import com.jantursky.kapow.effects.rainPieces
import com.jantursky.kapow.particles.ParticleSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConfettiRainMathTest {
    @Test
    fun piecesComeInAtTheDensityAndTheRestIsCarriedOver() {
        val (none, half) = rainPieces(0f, 30f, 1f / 60f)
        assertEquals(0, none)
        assertTrue(half in 0.49f..0.51f)
        val (whole, rest) = rainPieces(0.5f, 30f, 1f / 60f)
        assertEquals(1, whole)
        assertTrue(rest < 0.01f)
    }

    @Test
    fun overASecondTheDensityIsReached() {
        var carry = 0f
        var total = 0
        repeat(60) {
            val (n, rest) = rainPieces(carry, 25f, 1f / 60f)
            total += n
            carry = rest
        }
        assertTrue(total in 24..25, "$total pieces")
    }

    @Test
    fun theCapHoldsBackWhatDoesNotFit() {
        assertEquals(5, rainBudget(live = 10, wanted = 5))
        assertEquals(3, rainBudget(live = MAX_RAIN_PIECES - 3, wanted = 10))
        assertEquals(0, rainBudget(live = MAX_RAIN_PIECES, wanted = 10))
        assertEquals(0, rainBudget(live = MAX_RAIN_PIECES + 5, wanted = 10))
    }

    @Test
    fun piecesStartAlongTheTopEdgeAndFall() {
        val system = ParticleSystem(rainEmission(40, 200f, Density(1f)), 3)
        for (i in 0 until system.count) {
            assertTrue(system.x[i] in -100f..100f, "inside the width: ${system.x[i]}")
            assertEquals(0f, system.y[i])
        }
        system.step(1f)
        assertTrue((0 until system.count).all { system.y[it] > 0f }, "every piece moved down")
    }

    @Test
    fun theFactoryChecksItsArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.confettiRain(colors = emptyList()) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.confettiRain(density = 0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.confettiRain(density = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.confettiRain(size = 0.dp) }
        ConditionalEffect.confettiRain(listOf(Color.Red), 100f, 4.dp, seed = 1)
    }
}
