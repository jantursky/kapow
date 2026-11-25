package com.jantursky.kapow.samples.gallery

import androidx.compose.ui.input.pointer.PointerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TiltHintTest {
    @Test
    fun aTouchPointerIsTheFingerGesture() {
        assertTrue(isTouch(PointerType.Touch))
        assertFalse(isTouch(PointerType.Mouse))
        assertFalse(isTouch(PointerType.Stylus))
    }

    @Test
    fun theHintNamesTheGestureForTheLastPointer() {
        assertEquals("Move the pointer over the card to tilt it.", tiltHint(touch = false))
        assertEquals("Press and drag on the card to tilt it.", tiltHint(touch = true))
    }
}
