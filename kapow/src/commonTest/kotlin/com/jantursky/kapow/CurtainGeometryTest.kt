package com.jantursky.kapow

import com.jantursky.kapow.transitions.curtainFold
import com.jantursky.kapow.transitions.curtainOpening
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurtainGeometryTest {
    @Test
    fun theHalvesPartFromTheMiddlePastTheEdges() {
        assertEquals(0f, curtainOpening(200f, 3f, 0f))
        assertEquals(51.5f, curtainOpening(200f, 3f, 0.5f))
        assertEquals(103f, curtainOpening(200f, 3f, 1f))
        assertEquals(103f, curtainOpening(200f, 3f, 2f))
    }

    @Test
    fun theFoldsStayWithinTheirDepth() {
        assertEquals(0f, curtainFold(0f, 3f, 28f))
        assertEquals(3f, curtainFold(7f, 3f, 28f), 1e-4f)
        assertTrue((0..200).all { abs(curtainFold(it.toFloat(), 3f, 28f)) <= 3f })
    }
}
