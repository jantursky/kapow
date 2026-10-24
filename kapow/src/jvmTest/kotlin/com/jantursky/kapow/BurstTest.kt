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
class BurstTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.burst(
        effect: ChangeEffect = ChangeEffect.burst(Color.Red),
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, effect))
            }
        }
    }

    /** Pixels outside the element that are not the white background. */
    private fun ImageBitmap.paintedAround(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 0 until 400) {
            for (x in 0 until 400) {
                val inside = x in 150 until 250 && y in 150 until 250
                if (!inside && pixels[x, y] != Color.White) n++
            }
        }
        return n
    }

    @Test
    fun theBurstPopsOutBehindTheElementAndFades() =
        runEffectTest {
            burst()
            value++
            mainClock.advanceTimeBy(200)
            val image = capture()
            assertTrue(image.paintedAround() > 5000, "spikes around the element: ${image.paintedAround()}")
            assertEquals(Cobalt, image.colorAt(200, 200), "the burst is behind the element")
            mainClock.advanceTimeBy(600)
            assertEquals(0, capture().paintedAround())
        }

    @Test
    fun theSpikesLeaveGapsBetweenThem() =
        runEffectTest {
            burst(ChangeEffect.burst(Color.Red, spikes = 5))
            value++
            mainClock.advanceTimeBy(200)
            // Along a circle past the element's corners, the five spikes are met with background between them.
            val image = capture()
            var crossings = 0
            var inside = image.colorAt(200 + 80, 200) != Color.White
            for (step in 1..360) {
                val angle = step * kotlin.math.PI / 180
                val x = 200 + (80 * kotlin.math.cos(angle)).toInt()
                val y = 200 + (80 * kotlin.math.sin(angle)).toInt()
                val painted = image.colorAt(x, y) != Color.White
                if (painted && !inside) crossings++
                inside = painted
            }
            assertEquals(5, crossings)
        }

    @Test
    fun reducedMotionBrightensWithoutABurst() =
        runEffectTest {
            burst(motion = KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(0, image.paintedAround())
            assertTrue(image.colorAt(200, 200).luminance > Cobalt.luminance + 0.05f)
            mainClock.advanceTimeBy(500)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun tooFewSpikesAreRejected() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.burst(spikes = 2) }
    }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            burst()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun keyFrame() =
        runEffectTest {
            burst(ChangeEffect.burst())
            value++
            mainClock.advanceTimeBy(250)
            onRoot().captureRoboImage("src/jvmTest/screenshots/burst_250.png")
        }
}
