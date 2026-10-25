package com.jantursky.kapow

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.LinearEasing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class TransitionTuningTest {
    @Test
    fun easingAndReducedMotionAreValues() {
        assertEquals(KapowEnter.iris().easing(EaseIn), KapowEnter.iris().easing(EaseIn))
        assertEquals(KapowEnter.iris().easing(EaseIn).hashCode(), KapowEnter.iris().easing(EaseIn).hashCode())
        assertNotEquals(KapowEnter.iris().easing(EaseIn), KapowEnter.iris().easing(LinearEasing))
        assertNotEquals(KapowEnter.iris(), KapowEnter.iris().easing(EaseIn))
        assertEquals(
            KapowExit.dissolve().reducedMotion(ReducedMotionMode.Nothing),
            KapowExit.dissolve().reducedMotion(ReducedMotionMode.Nothing),
        )
        assertNotEquals(KapowExit.dissolve(), KapowExit.dissolve().reducedMotion(ReducedMotionMode.EndState))
        assertNotEquals(
            KapowExit.dissolve().reducedMotion(ReducedMotionMode.Fade),
            KapowExit.dissolve().reducedMotion(ReducedMotionMode.EndState),
        )
    }

    @Test
    fun theWrappersCombineInAnyOrder() {
        val a =
            KapowEnter
                .wipe()
                .delay(100.milliseconds)
                .easing(EaseIn)
                .reducedMotion(ReducedMotionMode.Fade)
        val b =
            KapowEnter
                .wipe()
                .reducedMotion(ReducedMotionMode.Fade)
                .easing(EaseIn)
                .delay(100.milliseconds)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun theTextShowsOnlyWhatIsSet() {
        assertEquals("KapowExit(None, 0s)", KapowExit.None.toString())
        val text =
            KapowExit.None
                .delay(5.milliseconds)
                .reducedMotion(ReducedMotionMode.Nothing)
                .toString()
        assertTrue(text.contains("delay=5ms") && text.contains("reducedMotion=Nothing"), text)
    }
}
