package com.jantursky.kapow

import com.jantursky.kapow.effects.spinImpulse
import com.jantursky.kapow.effects.spinShade
import com.jantursky.kapow.effects.spinTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpinMathTest {
    @Test
    fun aSpinFromRestTurnsWholeTurns() {
        assertEquals(360f, spinTarget(0f, null, turns = 1))
        assertEquals(720f, spinTarget(0f, null, turns = 2))
    }

    @Test
    fun aChangeMidSpinAddsATurn() {
        assertEquals(1080f, spinTarget(400f, previousTarget = 720f, turns = 1))
        assertEquals(1080f, spinTarget(700f, previousTarget = 720f, turns = 1))
    }

    @Test
    fun theTargetIsAlwaysAWholeTurn() {
        for (current in listOf(0f, 13f, 359f, 361f, 900f)) assertEquals(0f, spinTarget(current, null, 1) % 360f)
    }

    @Test
    fun fastSpinsKickHarder() {
        assertTrue(spinImpulse(SpinRate.Fast) > spinImpulse(SpinRate.Default))
    }

    @Test
    fun theElementDarkensAsItTurnsEdgeOn() {
        assertEquals(0f, spinShade(0f))
        assertEquals(0f, spinShade(360f), 1e-5f)
        assertEquals(0.35f, spinShade(90f), 1e-5f)
        assertTrue(spinShade(45f) in 0.05f..0.2f)
    }
}
