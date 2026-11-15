package com.jantursky.kapow.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformReadersTest {
    @Test
    fun desktopHasNoReducedMotionSetting() {
        assertFalse(platformReducedMotion())
    }

    @Test
    fun blurIsSupported() {
        assertTrue(blurSupported())
    }
}
