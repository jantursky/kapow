package com.jantursky.kapow.platform

import android.animation.ValueAnimator
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ReducedMotionTest {
    @After
    fun restore() {
        setDurationScale(1f)
    }

    @Test
    @Config(sdk = [26])
    fun animationsOffMeansReducedMotion() {
        setDurationScale(0f)
        assertTrue(platformReducedMotion())
    }

    @Test
    @Config(sdk = [26])
    fun animationsOnMeansFullMotion() {
        assertFalse(platformReducedMotion())
    }

    @Test
    @Config(sdk = [23])
    fun beforeApi26TheNodeReliesOnTheDurationScale() {
        setDurationScale(0f)
        assertFalse(platformReducedMotion())
    }

    // The system sets this from "Animator duration scale" in developer options. It's hidden API, but the
    // Robolectric Android jars expose it.
    private fun setDurationScale(scale: Float) {
        ValueAnimator::class.java.getMethod("setDurationScale", Float::class.javaPrimitiveType).invoke(null, scale)
    }
}
