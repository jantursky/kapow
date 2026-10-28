package com.jantursky.kapow

import kotlin.test.Test
import kotlin.test.assertEquals

class KapowTriggerTest {
    @Test
    fun eachFireRaisesTheCount() {
        val trigger = KapowTrigger()
        assertEquals(0L, trigger.count)
        trigger.fire()
        trigger.fire()
        assertEquals(2L, trigger.count)
    }
}
