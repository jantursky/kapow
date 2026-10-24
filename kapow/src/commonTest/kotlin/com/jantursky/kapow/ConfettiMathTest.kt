package com.jantursky.kapow

import androidx.compose.ui.unit.Density
import com.jantursky.kapow.effects.confettiEmission
import com.jantursky.kapow.effects.confettiFlip
import com.jantursky.kapow.effects.confettiOpacity
import com.jantursky.kapow.effects.confettiPiece
import com.jantursky.kapow.particles.ParticleSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfettiMathTest {
    private val density = Density(2f)

    @Test
    fun confettiGoesUpFirstThenFallsAndIsGoneWithinTwoSeconds() {
        val system = ParticleSystem(confettiEmission(20, 200f, density), seed = 4)
        repeat(9) { system.step(1f / 60) }
        val risen = (0 until system.count).count { system.y[it] < 0f }
        assertTrue(risen >= 18, "most pieces are thrown upward: $risen")
        val peak = (0 until system.count).minOf { system.y[it] }
        repeat(70) { system.step(1f / 60) }
        assertTrue((0 until system.count).any { system.y[it] > peak + 100f }, "gravity brings pieces back down")
        repeat(30) { system.step(1f / 60) }
        assertFalse(system.step(1f / 60), "all gone after two seconds")
    }

    @Test
    fun piecesStartSpreadAcrossTheMiddleOfTheElement() {
        val system = ParticleSystem(confettiEmission(30, 200f, density), seed = 1)
        for (i in 0 until system.count) assertTrue(system.x[i] in -50f..50f, "piece $i starts at ${system.x[i]}")
        assertTrue(
            (0 until system.count).any { system.x[it] < -20f } && (0 until system.count).any { system.x[it] > 20f },
        )
    }

    @Test
    fun aTumblingPieceSwingsBetweenFullWidthAndAThinEdge() {
        val widths = (0 until 360).map { confettiFlip(it.toFloat(), 0f) }
        assertEquals(1f, widths.max(), 0.01f)
        assertTrue(widths.min() in 0.1f..0.2f, "never vanishes: ${widths.min()}")
    }

    @Test
    fun piecesFadeOnlyAtTheEnd() {
        assertEquals(1f, confettiOpacity(0f))
        assertEquals(1f, confettiOpacity(0.6f))
        assertEquals(0.5f, confettiOpacity(0.83f), 0.01f)
        assertEquals(0f, confettiOpacity(1f), 0.001f)
    }

    @Test
    fun everyThirdPieceIsARibbon() {
        val square = confettiPiece(0, 10f)
        val ribbon = confettiPiece(2, 10f)
        assertTrue(ribbon.height / ribbon.width > 3f)
        assertTrue(square.height / square.width < 2f)
    }
}
