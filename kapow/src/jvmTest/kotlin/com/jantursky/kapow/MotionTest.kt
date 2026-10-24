package com.jantursky.kapow

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MotionTest {
    @Test
    fun anOverrideWinsOverThePlatform() {
        assertFalse(resolveReducedMotion(KapowMotion.Full, durationScale = 0f))
        assertTrue(resolveReducedMotion(KapowMotion.Reduced, durationScale = 1f))
    }

    @Test
    fun systemFollowsTheDurationScale() {
        assertTrue(resolveReducedMotion(KapowMotion.System, durationScale = 0f))
        // The JVM has no system setting of its own.
        assertFalse(resolveReducedMotion(KapowMotion.System, durationScale = 1f))
    }

    @Test
    fun isReducedMotionReadsTheComposition() =
        runEffectTest {
            val seen = mutableListOf<Boolean>()
            setEffectContent(KapowMotion.System) {
                seen += isReducedMotion()
                CompositionLocalProvider(LocalKapowMotion provides KapowMotion.Reduced) { seen += isReducedMotion() }
            }
            assertEquals(listOf(false, true), seen)
        }
}
