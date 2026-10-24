package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.turnedSweepStops
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration

class RainbowBorderMathTest {
    private val colors = listOf(Color.Red, Color.Green, Color.Blue)

    @Test
    fun unturnedStopsStartAtTheFirstColor() {
        val stops = turnedSweepStops(colors, 0f)
        assertEquals(0f to Color.Red, stops.first())
        assertEquals(1f to Color.Red, stops.last())
        assertEquals(listOf(0f, 0f, 1f / 3, 2f / 3, 1f), stops.map { it.first })
    }

    @Test
    fun turningMovesEveryColorAndKeepsTheSeamSmooth() {
        val stops = turnedSweepStops(colors, 0.5f)
        val positions = stops.map { it.first }
        assertEquals(positions.sorted(), positions)
        assertEquals(0f, positions.first())
        assertEquals(1f, positions.last())
        assertEquals(stops.first().second, stops.last().second, "both ends meet")
        assertTrue(stops.any { it.first == 0.5f && it.second == Color.Red }, "red turned half way")
    }

    @Test
    fun oneColorIsASolidStroke() {
        assertEquals(listOf(0f to Color.Red, 1f to Color.Red), turnedSweepStops(listOf(Color.Red), 0.3f).toList())
    }

    @Test
    fun theFactoryChecksItsArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.rainbowBorder(emptyList()) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.rainbowBorder(width = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.rainbowBorder(duration = Duration.ZERO) }
        assertEquals(6, ConditionalEffect.RainbowColors.size)
    }
}
