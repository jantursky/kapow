package com.jantursky.kapow

import com.jantursky.kapow.text.countValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CounterMathTest {
    @Test
    fun theEndsAreExact() {
        assertEquals(0, countValue(0, 1000, 0f))
        assertEquals(1000, countValue(0, 1000, 1f))
        assertEquals(Long.MAX_VALUE, countValue(0, Long.MAX_VALUE, 1f))
        assertEquals(-7, countValue(50, -7, 1f))
    }

    @Test
    fun theValueMovesMonotonicallyUp() {
        var last = 0L
        for (i in 0..200) {
            val v = countValue(0, 1000, i / 200f)
            assertTrue(v >= last)
            last = v
        }
        assertEquals(1000, last)
    }

    @Test
    fun theValueMovesMonotonicallyDown() {
        var last = 1000L
        for (i in 0..200) {
            val v = countValue(1000, -250, i / 200f)
            assertTrue(v <= last)
            last = v
        }
        assertEquals(-250, last)
    }

    @Test
    fun theMiddleIsHalfWay() {
        assertEquals(500, countValue(0, 1000, 0.5f))
        assertEquals(150, countValue(100, 200, 0.5f))
    }
}
