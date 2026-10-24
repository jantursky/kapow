package com.jantursky.kapow

import com.jantursky.kapow.transitions.DUST_COLUMNS
import com.jantursky.kapow.transitions.DUST_SWEEP
import com.jantursky.kapow.transitions.dustMoment
import com.jantursky.kapow.transitions.dustRise
import kotlin.test.Test
import kotlin.test.assertTrue

class DisintegrateGeometryTest {
    @Test
    fun everyGrainGoesDuringTheSweep() {
        for (column in 0 until 40) for (row in 0 until 10) assertTrue(dustMoment(column, row, 40) in 0f..DUST_SWEEP)
    }

    @Test
    fun theLeftGoesBeforeTheRight() {
        val left = (0 until 10).map { dustMoment(0, it, 40) }
        val right = (0 until 10).map { dustMoment(39, it, 40) }
        assertTrue(left.max() < right.min())
    }

    @Test
    fun dustRisesColumnByColumnWhileTheBandSweeps() {
        val moments = (0 until DUST_COLUMNS).map { dustRise(it, DUST_COLUMNS) }
        assertTrue(moments.zipWithNext().all { (a, b) -> b > a })
        assertTrue(moments.first() > 0f && moments.last() < DUST_SWEEP)
    }
}
