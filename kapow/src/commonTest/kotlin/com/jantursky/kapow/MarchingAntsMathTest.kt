package com.jantursky.kapow

import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.fitDashes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MarchingAntsMathTest {
    @Test
    fun aWholeNumberOfDashesFitsAround() {
        val (dash, gap) = fitDashes(103f, 6f, 4f)
        val repeats = 103f / (dash + gap)
        assertTrue(abs(repeats - 10f) < 1e-3f, "$repeats repeats")
        assertTrue(abs(dash / gap - 1.5f) < 1e-4f, "the pattern keeps its proportions")
    }

    @Test
    fun aShortOutlineGetsOneDash() {
        val (dash, gap) = fitDashes(3f, 6f, 4f)
        assertTrue(abs(dash + gap - 3f) < 1e-4f)
    }

    @Test
    fun nothingToFitLeavesThePatternAlone() {
        assertEquals(6f to 4f, fitDashes(0f, 6f, 4f))
    }

    @Test
    fun theFactoryChecksItsArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.marchingAnts(width = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.marchingAnts(dash = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.marchingAnts(gap = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.marchingAnts(speed = (-1).dp) }
    }
}
