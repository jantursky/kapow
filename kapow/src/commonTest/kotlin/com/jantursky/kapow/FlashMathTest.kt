package com.jantursky.kapow

import com.jantursky.kapow.effects.flashStrength
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlashMathTest {
    @Test
    fun theFlashSnapsUpThenFadesBackToNothing() {
        assertEquals(0f, flashStrength(0f))
        assertEquals(0.5f, flashStrength(0.05f), 0.0001f)
        assertEquals(1f, flashStrength(0.1f))
        assertEquals(0f, flashStrength(1f))
        assertEquals(0f, flashStrength(3f))
    }

    @Test
    fun theFadeSlowsDownAtTheEnd() {
        val early = flashStrength(0.2f) - flashStrength(0.3f)
        val late = flashStrength(0.8f) - flashStrength(0.9f)
        assertTrue(early > late)
        assertEquals(0.25f, flashStrength(0.55f), 0.0001f)
    }
}
