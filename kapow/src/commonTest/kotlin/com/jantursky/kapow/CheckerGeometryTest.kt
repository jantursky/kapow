package com.jantursky.kapow

import com.jantursky.kapow.transitions.CHECKER_WAVE
import com.jantursky.kapow.transitions.checkerOpening
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckerGeometryTest {
    @Test
    fun theFirstColorOpensFirst() {
        assertEquals(0f, checkerOpening(0, 0, 0f))
        assertEquals(0.5f, checkerOpening(0, 0, CHECKER_WAVE / 2), 1e-5f)
        assertEquals(0f, checkerOpening(1, 0, CHECKER_WAVE / 2))
        assertEquals(1f, checkerOpening(2, 4, CHECKER_WAVE))
    }

    @Test
    fun theSecondColorOpensLastAndFinishesAtTheEnd() {
        assertTrue(checkerOpening(0, 1, 0.5f) > 0f, "the waves overlap")
        assertEquals(1f, checkerOpening(0, 1, 1f))
        assertTrue(checkerOpening(0, 1, 0.99f) < 1f)
    }
}
