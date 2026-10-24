package com.jantursky.kapow

import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.beamTailAlpha
import com.jantursky.kapow.effects.loopPhase
import com.jantursky.kapow.effects.wrappedSpans
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration

class BorderBeamMathTest {
    @Test
    fun aLoopPhaseWrapsEveryPeriod() {
        assertEquals(0f, loopPhase(0.0, 2.0))
        assertEquals(0.25f, loopPhase(0.5, 2.0))
        assertEquals(0.25f, loopPhase(4.5, 2.0))
        assertEquals(0f, loopPhase(3.0, 0.0))
    }

    @Test
    fun spansStayOnTheOutline() {
        assertContentEquals(floatArrayOf(10f, 30f), wrappedSpans(10f, 30f, 100f))
        assertContentEquals(floatArrayOf(90f, 100f, 0f, 20f), wrappedSpans(-10f, 20f, 100f))
        assertContentEquals(floatArrayOf(90f, 100f, 0f, 5f), wrappedSpans(90f, 105f, 100f))
        assertContentEquals(floatArrayOf(0f, 100f), wrappedSpans(0f, 150f, 100f))
        assertEquals(0, wrappedSpans(10f, 10f, 100f).size)
        assertEquals(0, wrappedSpans(0f, 10f, 0f).size)
    }

    @Test
    fun theBeamBrightensTowardItsHead() {
        assertEquals(0f, beamTailAlpha(0f))
        assertEquals(1f, beamTailAlpha(1f))
        assertTrue(beamTailAlpha(0.3f) < beamTailAlpha(0.6f))
    }

    @Test
    fun theFactoryChecksItsArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.borderBeam(width = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.borderBeam(length = 0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.borderBeam(length = 1.5f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.borderBeam(duration = Duration.ZERO) }
        assertEquals(ConditionalEffect.borderBeam(), ConditionalEffect.borderBeam())
    }
}
