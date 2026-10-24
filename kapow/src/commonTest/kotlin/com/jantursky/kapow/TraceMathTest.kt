package com.jantursky.kapow

import com.jantursky.kapow.effects.clipSpan
import com.jantursky.kapow.effects.tailAlpha
import com.jantursky.kapow.effects.traceHead
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TraceMathTest {
    @Test
    fun theHeadRunsPastTheEndByTheTail() {
        assertEquals(0f, traceHead(0f, 400f, 100f))
        assertEquals(250f, traceHead(0.5f, 400f, 100f))
        assertEquals(500f, traceHead(1f, 400f, 100f))
        assertEquals(500f, traceHead(2f, 400f, 100f))
    }

    @Test
    fun spansAreClippedToTheOutline() {
        assertEquals(0f..30f, clipSpan(-20f, 30f, 400f))
        assertEquals(380f..400f, clipSpan(380f, 450f, 400f))
        assertEquals(10f..20f, clipSpan(10f, 20f, 400f))
        assertNull(clipSpan(-30f, -10f, 400f))
        assertNull(clipSpan(410f, 450f, 400f))
    }

    @Test
    fun theTailFadesFromTheHead() {
        assertEquals(1f, tailAlpha(0, 12))
        val alphas = (0 until 12).map { tailAlpha(it, 12) }
        assertTrue(alphas.zipWithNext().all { (a, b) -> b < a })
        assertTrue(alphas.last() < 0.1f)
    }
}
