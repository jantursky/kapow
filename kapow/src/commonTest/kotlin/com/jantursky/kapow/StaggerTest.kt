package com.jantursky.kapow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class StaggerTest {
    @Test
    fun delayGrowsWithTheIndexUpToTheMaximum() {
        assertEquals(Duration.ZERO, KapowStagger.delay(0, 50.milliseconds, 300.milliseconds))
        assertEquals(100.milliseconds, KapowStagger.delay(2, 50.milliseconds, 300.milliseconds))
        assertEquals(300.milliseconds, KapowStagger.delay(6, 50.milliseconds, 300.milliseconds))
        assertEquals(300.milliseconds, KapowStagger.delay(20, 50.milliseconds, 300.milliseconds))
    }

    @Test
    fun defaultsAre50MillisecondsUpTo500() {
        assertEquals(50.milliseconds, KapowStagger.delay(1))
        assertEquals(500.milliseconds, KapowStagger.delay(10))
        assertEquals(500.milliseconds, KapowStagger.delay(1000))
    }

    @Test
    fun rejectsNegativeInputs() {
        assertFailsWith<IllegalArgumentException> { KapowStagger.delay(-1) }
        assertFailsWith<IllegalArgumentException> { KapowStagger.delay(1, step = (-1).milliseconds) }
        assertFailsWith<IllegalArgumentException> { KapowStagger.delay(1, max = (-1).milliseconds) }
    }

    @Test
    fun aDelayIsPartOfTheTransitionsValue() {
        assertEquals(KapowEnter.iris().delay(100.milliseconds), KapowEnter.iris().delay(100.milliseconds))
        assertEquals(
            KapowEnter.iris().delay(100.milliseconds).hashCode(),
            KapowEnter.iris().delay(100.milliseconds).hashCode(),
        )
        assertNotEquals(KapowEnter.iris(), KapowEnter.iris().delay(100.milliseconds))
        assertEquals(KapowExit.iris().delay(100.milliseconds), KapowExit.iris().delay(100.milliseconds))
        assertNotEquals(KapowExit.iris(), KapowExit.iris().delay(100.milliseconds))
        assertTrue(
            KapowEnter
                .iris()
                .delay(100.milliseconds)
                .toString()
                .contains("100ms"),
        )
        assertEquals(KapowEnter.iris(), KapowEnter.iris().delay(Duration.ZERO))
    }

    @Test
    fun rejectsANegativeDelay() {
        assertFailsWith<IllegalArgumentException> { KapowEnter.iris().delay((-1).milliseconds) }
        assertFailsWith<IllegalArgumentException> { KapowExit.iris().delay((-1).milliseconds) }
    }
}
