package com.jantursky.kapow

import com.jantursky.kapow.effects.glitchFrame
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GlitchMathTest {
    @Test
    fun slicesCoverTheElementFromTopToBottomWithoutGaps() {
        val frame = glitchFrame(seed = 3, step = 2, height = 120f, offset = 8f)
        assertEquals(0f, frame.slices.first().top)
        assertEquals(120f, frame.slices.last().bottom)
        frame.slices.zipWithNext().forEach { (a, b) -> assertEquals(a.bottom, b.top) }
        assertTrue(frame.slices.size >= 4)
    }

    @Test
    fun shiftsAndTheSplitStayWithinTheOffset() {
        repeat(20) { step ->
            val frame = glitchFrame(seed = 1, step = step, height = 80f, offset = 10f)
            assertTrue(abs(frame.split) in 4f..10f, "split ${frame.split}")
            frame.slices.forEach { assertTrue(abs(it.shift) <= 15f, "shift ${it.shift}") }
        }
    }

    @Test
    fun theSameSeedAndStepGiveTheSameLook() {
        assertEquals(glitchFrame(5, 3, 100f, 6f), glitchFrame(5, 3, 100f, 6f))
        assertNotEquals(glitchFrame(5, 3, 100f, 6f), glitchFrame(5, 4, 100f, 6f))
    }

    @Test
    fun someSlicesStayAndSomeJump() {
        val slices = (0 until 10).flatMap { glitchFrame(2, it, 100f, 6f).slices }
        assertTrue(slices.any { it.shift == 0f })
        assertTrue(slices.any { it.shift != 0f })
    }
}
