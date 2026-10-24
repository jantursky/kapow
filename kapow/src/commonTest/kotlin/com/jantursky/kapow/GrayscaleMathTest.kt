package com.jantursky.kapow

import com.jantursky.kapow.effects.grayscaleSaturation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GrayscaleMathTest {
    @Test
    fun theSaturationFollowsTheFade() {
        assertEquals(1f, grayscaleSaturation(1f, 0f))
        assertEquals(0f, grayscaleSaturation(1f, 1f))
        assertEquals(0.5f, grayscaleSaturation(1f, 0.5f))
        assertEquals(0.75f, grayscaleSaturation(0.5f, 0.5f))
    }

    @Test
    fun theFactoryChecksItsArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.grayscale(-0.1f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.grayscale(1.1f) }
    }
}
