package com.jantursky.kapow

import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.spotlightClearance
import com.jantursky.kapow.effects.stackedLayerAlpha
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SpotlightMathTest {
    @Test
    fun stackedLayersAddUpToTheTotal() {
        val each = stackedLayerAlpha(0.6f, 8)
        val total = 1f - (1f - each).pow(8)
        assertTrue(abs(total - 0.6f) < 1e-4f, "$total")
        assertEquals(0.6f, stackedLayerAlpha(0.6f, 1))
        assertEquals(0f, stackedLayerAlpha(0f, 8))
    }

    @Test
    fun ringsClearFromThePaddingToTheSoftEdge() {
        assertEquals(8f, spotlightClearance(0, 5, 8f, 24f))
        assertEquals(32f, spotlightClearance(4, 5, 8f, 24f))
        assertEquals(20f, spotlightClearance(2, 5, 8f, 24f))
        assertEquals(8f, spotlightClearance(0, 1, 8f, 24f))
    }

    @Test
    fun theFactoryChecksItsArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.spotlight(padding = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.spotlight(softness = (-1).dp) }
    }
}
