package com.jantursky.kapow

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class TransitionTuningSceneTest {
    @Test
    fun anEasingReshapesTheProgress() {
        var shaped = 0f
        runTransitionTest {
            set(enter = KapowEnter.iris().easing(Easing { it * it * it }))
            show()
            advance(225)
            shaped = shown()
        }
        runTransitionTest {
            set(enter = KapowEnter.iris().easing(LinearEasing))
            show()
            advance(225)
            val linear = shown()
            assertTrue(shaped < linear, "a cubic curve is behind a linear one at half time: $shaped vs $linear")
            advance(3000)
            assertEquals(1f, shown())
        }
    }

    @Test
    fun anEasingReachesTheEndOnAnExit() =
        runTransitionTest {
            hideFromShown(KapowExit.iris().easing(LinearEasing))
            advance(225)
            val mid = shown()
            assertTrue(mid > 0.05f && mid < 0.95f, "$mid")
            advance(3000)
            assertEquals(0f, shown())
        }

    @Test
    fun reducedMotionNothingAndEndStateSkipTheMotion() {
        for (mode in listOf(ReducedMotionMode.Nothing, ReducedMotionMode.EndState)) {
            runTransitionTest {
                set(
                    enter = KapowEnter.iris().delay(400.milliseconds).reducedMotion(mode),
                    exit = KapowExit.iris().delay(400.milliseconds).reducedMotion(mode),
                    motion = KapowMotion.Reduced,
                )
                show()
                advance(64)
                assertEquals(1f, shown(), "$mode shows the content at once, without the delay")
                visible = false
                advance(64)
                assertEquals(0f, shown(), "$mode removes the content at once")
            }
        }
    }

    @Test
    fun reducedMotionFadeAndDefaultFade() {
        for (mode in listOf(ReducedMotionMode.Default, ReducedMotionMode.Fade)) {
            runTransitionTest {
                set(
                    enter = KapowEnter.iris().reducedMotion(mode),
                    exit = KapowExit.iris().reducedMotion(mode),
                    motion = KapowMotion.Reduced,
                )
                show()
                advance(150)
                val color = capture().colorAt(200, 200)
                assertNotEquals(Color.White, color, "$mode")
                assertNotEquals(Cobalt, color, "$mode fades")
            }
        }
    }

    @Test
    fun theOverrideIsIgnoredWithFullMotion() =
        runTransitionTest {
            set(enter = KapowEnter.iris().reducedMotion(ReducedMotionMode.Nothing))
            show()
            advance(100)
            val mid = shown()
            assertTrue(mid < 0.95f, "the iris still plays: $mid")
        }
}
