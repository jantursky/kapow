package com.jantursky.kapow.platform

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformReadersTest {
    @Test
    fun theReduceMotionSettingIsRead() {
        assertEquals(UIAccessibilityIsReduceMotionEnabled(), platformReducedMotion())
    }

    @Test
    fun blurIsSupported() {
        assertTrue(blurSupported())
    }
}
