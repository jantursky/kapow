package com.jantursky.kapow

import com.jantursky.kapow.text.appearMillis
import com.jantursky.kapow.text.cursorVisible
import com.jantursky.kapow.text.totalMillis
import com.jantursky.kapow.text.visibleCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypewriterMathTest {
    @Test
    fun charactersAppearAtTheConfiguredRate() {
        assertEquals(0, visibleCount("hello", 0, 10f, 0, 0))
        assertEquals(0, visibleCount("hello", 99, 10f, 0, 0))
        assertEquals(1, visibleCount("hello", 100, 10f, 0, 0))
        assertEquals(3, visibleCount("hello", 350, 10f, 0, 0))
        assertEquals(5, visibleCount("hello", 500, 10f, 0, 0))
        assertEquals(5, visibleCount("hello", 60_000, 10f, 0, 0))
    }

    @Test
    fun theStartDelayHoldsTheFirstCharacter() {
        assertEquals(0, visibleCount("hello", 399, 10f, 300, 0))
        assertEquals(1, visibleCount("hello", 400, 10f, 300, 0))
    }

    @Test
    fun punctuationHoldsTheNextCharacter() {
        // "a,b": a at 100, the comma at 200, then the pause, so b comes at 300 + 300.
        assertEquals(2, visibleCount("a,b", 450, 10f, 0, 300))
        assertEquals(2, visibleCount("a,b", 599, 10f, 0, 300))
        assertEquals(3, visibleCount("a,b", 600, 10f, 0, 300))
        assertEquals(600, totalMillis("a,b", 10f, 0, 300))
    }

    @Test
    fun theCountNeverGoesBackwards() {
        val text = "Hi, there. Ok!"
        var last = 0
        for (t in 0..5000L step 7) {
            val count = visibleCount(text, t, 12f, 150, 200)
            assertTrue(count >= last)
            last = count
        }
        assertEquals(text.length, last)
    }

    @Test
    fun totalsMatchWhenTheLastCharacterAppears() {
        assertEquals(500, totalMillis("hello", 10f, 0, 0))
        assertEquals(800, totalMillis("hello", 10f, 300, 0))
        assertEquals(300, totalMillis("", 10f, 300, 0))
        assertEquals(500, appearMillis("hello", 4, 10f, 0))
    }

    @Test
    fun theCursorIsSolidWhileTypingThenBlinks() {
        assertEquals(true, cursorVisible(0, 500, 400))
        assertEquals(true, cursorVisible(499, 500, 400))
        assertEquals(true, cursorVisible(500, 500, 400))
        assertEquals(false, cursorVisible(900, 500, 400))
        assertEquals(true, cursorVisible(1300, 500, 400))
    }
}
