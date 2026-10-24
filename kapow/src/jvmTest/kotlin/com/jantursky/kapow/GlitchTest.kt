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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class GlitchTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        effect: ChangeEffect = ChangeEffect.glitch(300.milliseconds, 8.dp),
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, effect))
            }
        }
    }

    /** Pixels of the element's area, and the 20 px around it, that are neither the element's color nor white. */
    private fun ImageBitmap.disturbed(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 130 until 270) {
            for (x in 130 until 270) {
                val c = pixels[x, y]
                if (c != Cobalt && c != Color.White) n++
            }
        }
        return n
    }

    @Test
    fun theElementSplitsIntoChannelsAndSlicesThenSnapsBack() =
        runEffectTest {
            scene()
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertTrue(image.disturbed() > 100, "color fringes: ${image.disturbed()}")
            assertEquals(Color.White, image.colorAt(140, 200), "nothing is drawn outside the element")
            mainClock.advanceTimeBy(400)
            val after = capture()
            assertEquals(0, after.disturbed())
        }

    @Test
    fun aZeroOffsetKeepsTheElementInPlace() =
        runEffectTest {
            scene(ChangeEffect.glitch(offset = 0.dp))
            value++
            mainClock.advanceTimeBy(100)
            assertEquals(0, capture().disturbed())
        }

    @Test
    fun reducedMotionBrightensWithoutShifting() =
        runEffectTest {
            scene(motion = KapowMotion.Reduced)
            val base = capture().colorAt(200, 200).luminance
            value++
            mainClock.advanceTimeBy(60)
            val image = capture()
            assertTrue(image.colorAt(200, 200).luminance > base + 0.05f)
            assertEquals(image.colorAt(200, 200), image.colorAt(152, 152), "evenly, nothing shifted")
            mainClock.advanceTimeBy(600)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(40) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun parametersAreChecked() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.glitch(duration = Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.glitch(offset = (-1).dp) }
    }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene()
            value++
            mainClock.advanceTimeBy(100)
            onRoot().captureRoboImage("src/jvmTest/screenshots/glitch_100.png")
        }
}
