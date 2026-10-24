package com.jantursky.kapow

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class ShineTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun androidx.compose.ui.test.ComposeUiTest.shine(motion: KapowMotion = KapowMotion.Full) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(Modifier.changeEffect(value, ChangeEffect.shine(angle = 0f, duration = 1.seconds)))
            }
        }
    }

    private fun rowLuminance(image: androidx.compose.ui.graphics.ImageBitmap) = (0 until 100).map { image.colorAt(it, 50).luminance }

    @Test
    fun theBandBrightensPartOfTheElementMidway() =
        runEffectTest {
            shine()
            val base = capture().colorAt(50, 50).luminance
            value++
            mainClock.advanceTimeBy(500)
            val row = rowLuminance(capture())
            assertTrue(row.max() > base + 0.15f, "brightest ${row.max()} vs base $base")
            assertTrue(row.min() < base + 0.02f, "the band covers only part of the element")
        }

    @Test
    fun theBandStaysOnTheElementsPixels() =
        runEffectTest {
            shine()
            value++
            mainClock.advanceTimeBy(500)
            assertEquals(Color.White, capture().colorAt(150, 50))
        }

    @Test
    fun nothingIsLeftAfterTheSweep() =
        runEffectTest {
            shine()
            val before = capture().colorAt(50, 50)
            value++
            mainClock.advanceTimeBy(1200)
            assertTrue(rowLuminance(capture()).all { kotlin.math.abs(it - before.luminance) < 0.01f })
        }

    @Test
    fun reducedMotionFlashesTheWholeElementInsteadOfSweeping() =
        runEffectTest {
            shine(KapowMotion.Reduced)
            val base = capture().colorAt(50, 50).luminance
            value++
            mainClock.advanceTimeBy(50)
            val row = rowLuminance(capture())
            assertTrue(row.min() > base + 0.05f, "every pixel brightens")
            assertTrue(row.max() - row.min() < 0.01f, "evenly, no band")
            mainClock.advanceTimeBy(600)
            val after = capture().colorAt(50, 50).luminance
            assertTrue(kotlin.math.abs(after - base) < 0.01f, "after $after base $base")
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            shine()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1, "recomposed ${counter.count - before} times")
        }

    @Test
    fun theCounterNoticesStateReadInComposition() =
        runEffectTest {
            // Control for framesDoNotRecompose: an animation read during composition recomposes every frame.
            var target by mutableIntStateOf(0)
            setEffectContent {
                val animated by animateFloatAsState(target.toFloat(), tween(1000))
                counter.Count()
                Element(Modifier.then(if (animated > 2f) Modifier else Modifier))
            }
            mainClock.advanceTimeByFrame()
            val before = counter.count
            target = 10
            repeat(30) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before > 10)
        }

    @Test
    fun keyFrames() =
        runEffectTest {
            shine()
            value++
            for (ms in listOf(300, 500, 700)) {
                mainClock.advanceTimeBy(if (ms == 300) 300 else 200)
                onRoot().captureRoboImage("src/jvmTest/screenshots/shine_$ms.png")
            }
        }
}
