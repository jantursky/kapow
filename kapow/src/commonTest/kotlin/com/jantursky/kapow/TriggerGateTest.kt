package com.jantursky.kapow

import com.jantursky.kapow.internal.TriggerGate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class TriggerGateTest {
    private val ms = 1_000_000L

    @Test
    fun theFirstTriggerIsAccepted() {
        assertTrue(TriggerGate().tryAccept(5 * ms, 500.milliseconds))
    }

    @Test
    fun triggersInsideTheCooldownAreRejected() {
        val gate = TriggerGate()
        gate.tryAccept(0, 500.milliseconds)
        assertFalse(gate.tryAccept(499 * ms, 500.milliseconds))
        assertTrue(gate.tryAccept(500 * ms, 500.milliseconds))
    }

    @Test
    fun aRejectedTriggerDoesNotRestartTheCooldown() {
        val gate = TriggerGate()
        gate.tryAccept(0, 500.milliseconds)
        gate.tryAccept(400 * ms, 500.milliseconds)
        assertTrue(gate.tryAccept(500 * ms, 500.milliseconds))
    }

    @Test
    fun noCooldownAcceptsEveryTrigger() {
        val gate = TriggerGate()
        repeat(3) { assertTrue(gate.tryAccept(0, Duration.ZERO)) }
    }
}
