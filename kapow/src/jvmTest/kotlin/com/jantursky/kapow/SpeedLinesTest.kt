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

@OptIn(ExperimentalTestApi::class)
class SpeedLinesTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.lines(motion: KapowMotion = KapowMotion.Full) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, ChangeEffect.speedLines(Color.Red)))
            }
        }
    }

    /** Painted pixels outside the element whose distance from it is in [band]. */
    private fun ImageBitmap.paintedAround(band: IntRange): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 0 until 400) {
            for (x in 0 until 400) {
                val dx = maxOf(150 - x, x - 249, 0)
                val dy = maxOf(150 - y, y - 249, 0)
                if (maxOf(dx, dy) in band && pixels[x, y] != Color.White) n++
            }
        }
        return n
    }

    @Test
    fun linesFlyOutFromTheEdgesAndVanish() =
        runEffectTest {
            lines()
            value++
            mainClock.advanceTimeBy(150)
            val image = capture()
            assertTrue(image.paintedAround(1..60) > 300, "lines around the element")
            assertEquals(0, image.paintedAround(1..3), "the lines start clear of the edges")
            assertEquals(Cobalt, image.colorAt(200, 200))
            mainClock.advanceTimeBy(500)
            assertEquals(0, capture().paintedAround(1..200))
        }

    @Test
    fun linesMoveOutward() =
        runEffectTest {
            lines()
            value++
            mainClock.advanceTimeBy(100)
            val early = capture().paintedAround(30..60)
            mainClock.advanceTimeBy(250)
            assertTrue(capture().paintedAround(30..60) > early, "more of the lines are far out later")
        }

    @Test
    fun reducedMotionBrightensWithoutLines() =
        runEffectTest {
            lines(KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(0, image.paintedAround(1..200))
            assertTrue(image.colorAt(200, 200).luminance > Cobalt.luminance + 0.05f)
            mainClock.advanceTimeBy(500)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun aNonPositiveCountIsRejected() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.speedLines(count = 0) }
    }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            lines()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(40) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun keyFrame() =
        runEffectTest {
            lines()
            value++
            mainClock.advanceTimeBy(150)
            onRoot().captureRoboImage("src/jvmTest/screenshots/speed_lines_150.png")
        }
}
