package com.jantursky.kapow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class TransitionDelayTest {
    @Test
    fun aDelayedEnterStaysHiddenThenPlaysItsFullDuration() =
        runTransitionTest {
            set(enter = KapowEnter.iris().delay(400.milliseconds))
            show()
            advance(300)
            assertEquals(0f, shown(), "hidden during the delay")
            advance(100 + 225)
            val mid = shown()
            assertTrue(mid > 0.05f && mid < 0.95f, "half way through its own 450 ms, not $mid")
            advance(3000)
            assertEquals(1f, shown())
        }

    @Test
    fun aDelayedExitStaysShownThenPlaysAndLeavesAfterIt() =
        runTransitionTest {
            hideFromShown(KapowExit.iris().delay(400.milliseconds))
            advance(300)
            assertEquals(1f, shown(), "fully shown during the delay, not removed early")
            advance(100 + 225)
            val mid = shown()
            assertTrue(mid > 0.05f && mid < 0.95f, "half way through its own 450 ms, not $mid")
            advance(3000)
            val image = capture()
            assertEquals(0f, shown(image))
            assertEquals(0, others(image))
        }

    @Test
    fun noDelayPlaysAsBefore() =
        runTransitionTest {
            set(enter = KapowEnter.iris().delay(Duration.ZERO))
            show()
            advance(225)
            val mid = shown()
            assertTrue(mid > 0.05f && mid < 0.95f, "$mid")
        }

    @Test
    fun staggeredItemsStartOneAfterAnother() =
        runEffectTest {
            var visible by mutableStateOf(false)
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    for (i in 0 until 3) {
                        KapowAnimatedVisibility(
                            visible,
                            Modifier.offset((20 + i * 120).dp, 150.dp),
                            enter = KapowEnter.wipe().delay(KapowStagger.delay(i, 400.milliseconds, 2000.milliseconds)),
                        ) {
                            SolidElement(Modifier)
                        }
                    }
                }
            }
            visible = true
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(300)
            val early = capture()
            assertNotEquals(Color.White, early.colorAt(30, 200), "the first item is on its way")
            assertEquals(Color.White, early.colorAt(150, 200), "the second still waits")
            assertEquals(Color.White, early.colorAt(270, 200), "and the third")
            mainClock.advanceTimeBy(3000)
            val end = capture()
            for (x in listOf(30, 150, 270)) assertEquals(Cobalt, end.colorAt(x, 200))
        }

    @Test
    fun aDelayedEnterAndExitInAnimatedContent() =
        runEffectTest {
            var page by mutableStateOf(0)
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    AnimatedContent(
                        page,
                        Modifier.offset(150.dp, 150.dp),
                        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    ) { shown ->
                        SolidElement(
                            Modifier.kapowTransition(
                                this,
                                KapowEnter.iris().delay(400.milliseconds),
                                KapowExit.iris().delay(400.milliseconds),
                            ),
                            if (shown == 0) Color.Red else Cobalt,
                        )
                    }
                }
            }
            mainClock.advanceTimeBy(100)
            page = 1
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(250)
            val waiting = capture()
            assertEquals(Color.Red, waiting.colorAt(160, 200), "the old page stays whole during the delay")
            assertEquals(Color.Red, waiting.colorAt(200, 200), "and the new page is still hidden")
            mainClock.advanceTimeBy(3000)
            val end = capture()
            assertEquals(Cobalt, end.colorAt(160, 200))
            assertEquals(Cobalt, end.colorAt(200, 200))
        }
}
