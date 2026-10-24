package com.jantursky.kapow.platform

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

internal actual fun platformReducedMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
