package com.jantursky.kapow

import com.jantursky.kapow.effects.shimmerProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ShimmerMathTest {
    @Test
    fun theSweepRunsFromZeroToOneThenPauses() {
        assertEquals(0f, shimmerProgress(0.0, 1.5, 0.5))
        assertEquals(0.5f, shimmerProgress(0.75, 1.5, 0.5))
        assertNull(shimmerProgress(1.5, 1.5, 0.5))
        assertNull(shimmerProgress(1.9, 1.5, 0.5))
    }

    @Test
    fun theSweepLoops() {
        assertEquals(0f, shimmerProgress(2.0, 1.5, 0.5))
        assertEquals(0.5f, shimmerProgress(2.75, 1.5, 0.5))
        assertNotNull(shimmerProgress(200.1, 1.5, 0.5))
    }

    @Test
    fun withoutAPauseItNeverRests() {
        for (t in 0..40) assertNotNull(shimmerProgress(t * 0.1, 1.0, 0.0))
    }

    @Test
    fun theFactoryChecksItsArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.shimmer(duration = Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.shimmer(bandWidth = 0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.shimmer(softness = 0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.shimmer(softness = 1.5f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.shimmer(pause = (-1).milliseconds) }
    }
}
