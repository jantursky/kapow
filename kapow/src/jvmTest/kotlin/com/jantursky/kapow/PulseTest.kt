package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
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

@OptIn(ExperimentalTestApi::class)
class PulseTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.pulse(
        effect: ChangeEffect = ChangeEffect.pulse(CircleShape, Color.Red, PulseStyle.Fill),
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, effect))
            }
        }
    }

    @Test
    fun aPulseGrowsOutsideTheElementAndFades() =
        runEffectTest {
            pulse()
            value++
            mainClock.advanceTimeBy(400)
            // Right of the element, inside the grown circle.
            assertNotEquals(Color.White, capture().colorAt(265, 200))
            mainClock.advanceTimeBy(2000)
            assertEquals(Color.White, capture().colorAt(265, 200))
        }

    @Test
    fun theShapeIsKept() =
        runEffectTest {
            pulse()
            value++
            mainClock.advanceTimeBy(400)
            // The grown circle doesn't reach the corner diagonal of its bounding box.
            assertEquals(Color.White, capture().colorAt(140, 140))
        }

    @Test
    fun severalPulsesFollowEachOther() =
        runEffectTest {
            pulse(ChangeEffect.pulse(CircleShape, Color.Red, PulseStyle.Stroke(4.dp), count = 2))
            value++
            mainClock.advanceTimeBy(150)
            val ringsEarly = ringCrossings()
            mainClock.advanceTimeBy(300)
            assertTrue(ringCrossings() > ringsEarly, "a second ring joins the first")
        }

    @Test
    fun reducedMotionOnlyTintsBehindTheElement() =
        runEffectTest {
            pulse(motion = KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            assertEquals(Color.White, capture().colorAt(265, 200))
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            pulse()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun keyFrame() =
        runEffectTest {
            pulse(ChangeEffect.pulse(CircleShape, Color.Red, PulseStyle.Stroke(3.dp), count = 3))
            value++
            mainClock.advanceTimeBy(600)
            onRoot().captureRoboImage("src/jvmTest/screenshots/pulse_600.png")
        }

    /** Ring edges met along a row from the element's right side outward. */
    private fun ComposeUiTest.ringCrossings(): Int {
        val image = capture()
        var crossings = 0
        var inside = false
        for (x in 251 until 330) {
            val red = image.colorAt(x, 200) != Color.White
            if (red && !inside) crossings++
            inside = red
        }
        return crossings
    }
}
