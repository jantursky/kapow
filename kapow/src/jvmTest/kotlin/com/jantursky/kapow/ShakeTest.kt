package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class ShakeTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        effect: ChangeEffect,
        motion: KapowMotion = KapowMotion.Full,
        modifier: (Modifier) -> Modifier = { it.changeEffect(value, effect) },
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(modifier(Modifier.offset(150.dp, 150.dp)))
            }
        }
    }

    /** The left edge of the element: the first column from 100 on that is cobalt. */
    private fun ComposeUiTest.leftEdge(): Int {
        val image = capture()
        return (100 until 300).first { image.colorAt(it, 200) == Cobalt }
    }

    @Test
    fun theElementSwingsPastItsPlaceAndEndsThere() =
        runEffectTest {
            scene(ChangeEffect.shake(distance = 20.dp, duration = 400.milliseconds))
            assertEquals(150, leftEdge())
            value++
            val edges = mutableSetOf<Int>()
            repeat(20) {
                mainClock.advanceTimeBy(20)
                edges += leftEdge()
            }
            assertTrue(edges.any { it < 148 }, "swings left: $edges")
            assertTrue(edges.any { it > 152 }, "swings right: $edges")
            mainClock.advanceTimeBy(2000)
            assertEquals(150, leftEdge())
        }

    @Test
    fun aVerticalShakeLeavesTheSidesAlone() =
        runEffectTest {
            scene(ChangeEffect.shake(ShakeAxis.Vertical, distance = 20.dp, duration = 400.milliseconds))
            value++
            repeat(20) {
                mainClock.advanceTimeBy(20)
                assertEquals(150, leftEdge())
            }
        }

    @Test
    fun aRotationTurnsTheElementAndEndsUpright() =
        runEffectTest {
            scene(ChangeEffect.shake(distance = 0.dp, rotation = 20f, duration = 400.milliseconds))
            value++
            var turned = false
            repeat(20) {
                mainClock.advanceTimeBy(20)
                turned = turned || capture().colorAt(152, 152) != Cobalt
            }
            assertTrue(turned, "a corner leaves its place")
            mainClock.advanceTimeBy(2000)
            assertEquals(Cobalt, capture().colorAt(152, 152))
            assertEquals(Color.White, capture().colorAt(254, 152))
        }

    @Test
    fun aChangeMidShakeCarriesOnWithoutAJump() =
        runEffectTest {
            scene(ChangeEffect.shake(distance = 30.dp, duration = 600.milliseconds))
            value++
            mainClock.advanceTimeBy(60)
            val before = leftEdge()
            value++
            mainClock.advanceTimeByFrame()
            val after = leftEdge()
            assertTrue(kotlin.math.abs(after - before) <= 12, "$before then $after")
            mainClock.advanceTimeBy(3000)
            assertEquals(150, leftEdge())
        }

    @Test
    fun nothingPlaysOnTheFirstComposition() =
        runEffectTest {
            scene(ChangeEffect.shake(distance = 30.dp))
            repeat(10) {
                mainClock.advanceTimeBy(30)
                assertEquals(150, leftEdge())
            }
        }

    @Test
    fun theWrappersApply() =
        runEffectTest {
            scene(ChangeEffect.shake(distance = 30.dp, duration = 400.milliseconds).delay(300.milliseconds))
            value++
            mainClock.advanceTimeBy(100)
            assertEquals(150, leftEdge(), "still waiting")
            var moved = false
            repeat(15) {
                mainClock.advanceTimeBy(20)
                moved = moved || leftEdge() != 150
            }
            assertTrue(moved, "starts after the delay")
        }

    @Test
    fun theFeedbackEventIsSentOncePerPlayAlsoWithReducedMotion() {
        for (motion in listOf(KapowMotion.Full, KapowMotion.Reduced)) {
            val events = mutableListOf<KapowFeedbackEvent>()
            runEffectTest {
                setEffectContent(motion) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalKapowFeedback provides KapowFeedback { events += it },
                    ) {
                        Box(Modifier.fillMaxSize().background(Color.White)) {
                            SolidElement(
                                Modifier
                                    .offset(150.dp, 150.dp)
                                    .changeEffect(value, ChangeEffect.shake(), feedback = KapowFeedbackEvent.Success),
                            )
                        }
                    }
                }
                value++
                mainClock.advanceTimeBy(2000)
            }
            assertEquals(1, events.size, "$motion: $events")
        }
    }

    @Test
    fun reducedMotionBrightensWithoutMoving() =
        runEffectTest {
            scene(ChangeEffect.shake(distance = 30.dp), KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(Color.White, image.colorAt(148, 200))
            assertNotEquals(Color.White, image.colorAt(152, 200))
            assertTrue(image.colorAt(200, 200).luminance > Cobalt.luminance + 0.05f)
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene(ChangeEffect.shake())
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene(ChangeEffect.shake(distance = 24.dp, rotation = 6f, duration = 600.milliseconds))
            value++
            mainClock.advanceTimeBy(120)
            onRoot().captureRoboImage("src/jvmTest/screenshots/shake_120.png")
        }
}
