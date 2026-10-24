package com.jantursky.kapow.crease

import kotlin.test.Test
import kotlin.test.assertEquals

class HingeProgressTest {
    @Test
    fun theDefaultRangeMapsShutToFlat() {
        assertEquals(0f, hingeProgress(0f, 0f..180f))
        assertEquals(0.5f, hingeProgress(90f, 0f..180f))
        assertEquals(1f, hingeProgress(180f, 0f..180f))
    }

    @Test
    fun anglesOutsideTheRangeAreClamped() {
        assertEquals(0f, hingeProgress(10f, 45f..135f))
        assertEquals(1f, hingeProgress(170f, 45f..135f))
        assertEquals(0.5f, hingeProgress(90f, 45f..135f))
    }

    @Test
    fun anEmptyRangeIsAThreshold() {
        assertEquals(0f, hingeProgress(89f, 90f..90f))
        assertEquals(1f, hingeProgress(90f, 90f..90f))
    }
}
