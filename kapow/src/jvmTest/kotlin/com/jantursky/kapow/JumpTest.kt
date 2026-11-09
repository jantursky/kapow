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
class JumpTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        effect: ChangeEffect,
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(Modifier.offset(150.dp, 250.dp).changeEffect(value, effect))
            }
        }
    }

    /** The top edge of the element: the first row from 100 on that is cobalt in the middle column. */
    private fun ComposeUiTest.topEdge(): Int {
        val image = capture()
        return (100 until 390).first { image.colorAt(200, it) == Cobalt }
    }

    private fun ComposeUiTest.bottomEdge(): Int {
        val image = capture()
        return (399 downTo 100).first { image.colorAt(200, it) == Cobalt }
    }

    @Test
    fun theElementRisesByItsHeightAndRests() =
        runEffectTest {
            scene(ChangeEffect.jump(height = 60.dp, squash = 0f, duration = 400.milliseconds))
            assertEquals(250, topEdge())
            value++
            var highest = 250
            repeat(20) {
                mainClock.advanceTimeBy(20)
                highest = minOf(highest, topEdge())
            }
            assertTrue(highest in 186..194, "rises about 60: $highest")
            mainClock.advanceTimeBy(2000)
            assertEquals(250, topEdge())
            assertEquals(349, bottomEdge())
        }

    @Test
    fun twoHopsReachTheTopTwice() =
        runEffectTest {
            scene(ChangeEffect.jump(height = 60.dp, hops = 2, squash = 0f, duration = 800.milliseconds))
            value++
            val tops = mutableListOf<Int>()
            repeat(40) {
                mainClock.advanceTimeBy(20)
                tops += topEdge()
            }
            val peaks = tops.windowed(3).count { (a, b, c) -> b < a && b <= c && b < 200 }
            assertEquals(2, peaks, "$tops")
            mainClock.advanceTimeBy(2000)
            assertEquals(250, topEdge())
        }

    @Test
    fun theSquashFlattensTheLandingAndRecovers() =
        runEffectTest {
            scene(ChangeEffect.jump(height = 40.dp, squash = 1f, duration = 500.milliseconds))
            value++
            var flattest = 250
            repeat(30) {
                mainClock.advanceTimeBy(18)
                // Standing on the ground the bottom edge stays put while the top comes down.
                if (bottomEdge() == 349) flattest = maxOf(flattest.takeIf { it > 250 } ?: 250, topEdge())
            }
            assertTrue(flattest > 255, "flattened: $flattest")
            mainClock.advanceTimeBy(2000)
            assertEquals(250, topEdge())
            assertEquals(349, bottomEdge())
        }

    @Test
    fun reducedMotionBrightensWithoutMoving() =
        runEffectTest {
            scene(ChangeEffect.jump(height = 60.dp), KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(Color.White, image.colorAt(200, 247))
            assertNotEquals(Color.White, image.colorAt(200, 252))
            assertTrue(image.colorAt(200, 300).luminance > Cobalt.luminance + 0.05f)
        }

    @Test
    fun nothingPlaysOnTheFirstCompositionAndFramesDoNotRecompose() =
        runEffectTest {
            scene(ChangeEffect.jump(height = 60.dp))
            repeat(10) {
                mainClock.advanceTimeBy(30)
                assertEquals(250, topEdge())
            }
            val before = counter.count
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene(ChangeEffect.jump(height = 48.dp, squash = 1f, duration = 450.milliseconds))
            value++
            mainClock.advanceTimeBy(180)
            onRoot().captureRoboImage("src/jvmTest/screenshots/jump_180.png")
        }
}
