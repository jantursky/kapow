package com.jantursky.kapow

import com.jantursky.kapow.effects.puffGrowth
import com.jantursky.kapow.effects.puffOpacity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmokeMathTest {
    @Test
    fun puffsFadeInThenOut() {
        assertEquals(0f, puffOpacity(0f))
        assertEquals(0.7f, puffOpacity(0.2f))
        assertEquals(0f, puffOpacity(1f))
        assertTrue(puffOpacity(0.1f) < puffOpacity(0.2f))
        assertTrue(puffOpacity(0.6f) < puffOpacity(0.3f))
    }

    @Test
    fun puffsSpreadAsTheyRise() {
        assertTrue(puffGrowth(1f) > puffGrowth(0f))
    }
}
