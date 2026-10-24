package com.jantursky.kapow

import com.jantursky.kapow.transitions.tearEdge
import com.jantursky.kapow.transitions.tearWander
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class TearGeometryTest {
    private fun xs(edge: FloatArray) = edge.filterIndexed { i, _ -> i % 2 == 0 }

    private fun ys(edge: FloatArray) = edge.filterIndexed { i, _ -> i % 2 == 1 }

    @Test
    fun theEdgeSpansTheContentTopToBottom() {
        val ys = ys(tearEdge(100f, 10f, 0.5f))
        assertTrue(ys.first() < -100f && ys.last() > 100f)
        assertTrue(ys.zipWithNext().all { (a, b) -> b > a }, "goes down")
    }

    @Test
    fun theTearStaysWithinAToothOfTheMiddleAndIsJagged() {
        val xs = xs(tearEdge(100f, 10f, 0.5f))
        assertTrue(xs.all { it in -10f..10f })
        val turns = xs.zipWithNext { a, b -> b - a }.zipWithNext().count { (a, b) -> (a > 0f) != (b > 0f) }
        assertTrue(turns > xs.size / 2, "turns $turns times in ${xs.size} points")
    }

    @Test
    fun theWanderIsSmoothAndBounded() {
        val values = (0..400).map { tearWander(it / 40f) }
        assertTrue(values.all { it in -1f..1f })
        assertTrue(values.zipWithNext().all { (a, b) -> abs(b - a) < 0.1f })
    }

    @Test
    fun theTeethClearTheContentAtBothEnds() {
        assertTrue(xs(tearEdge(100f, 10f, 0f)).all { it <= -100f })
        assertTrue(xs(tearEdge(100f, 10f, 1f)).all { it >= 100f })
    }

    @Test
    fun theTearIsTheSameEveryTime() {
        assertContentEquals(tearEdge(80f, 12f, 0.3f), tearEdge(80f, 12f, 0.3f))
    }
}
