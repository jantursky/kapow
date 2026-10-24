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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration

@OptIn(ExperimentalTestApi::class)
class RippleTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.ripple(
        effect: ChangeEffect = ChangeEffect.ripple(Color.White.copy(alpha = 0.6f)),
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
    fun theInkSpreadsFromTheCenterAndFades() =
        runEffectTest {
            ripple()
            val base = Cobalt.luminance
            value++
            mainClock.advanceTimeBy(150)
            val image = capture()
            assertTrue(image.colorAt(200, 200).luminance > base + 0.2f, "the center is inked")
            assertEquals(Cobalt, image.colorAt(152, 152), "the corner is not reached yet")
            mainClock.advanceTimeBy(700)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun theInkStaysInsideTheShape() =
        runEffectTest {
            ripple(ChangeEffect.ripple(Color.Red, CircleShape, TransformOrigin(0f, 0f)))
            value++
            mainClock.advanceTimeBy(150)
            val image = capture()
            assertTrue(image.colorAt(170, 170).red > 0.9f, "inked inside the circle")
            assertEquals(Cobalt, image.colorAt(152, 152), "the corner outside the circle")
            assertEquals(Color.White, image.colorAt(147, 160), "outside the element")
        }

    @Test
    fun reducedMotionTintsTheWholeShapeWithoutSpreading() =
        runEffectTest {
            ripple(motion = KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(50)
            val image = capture()
            val center = image.colorAt(200, 200).luminance
            assertTrue(center > Cobalt.luminance + 0.1f)
            assertTrue(abs(image.colorAt(152, 152).luminance - center) < 0.01f, "evenly, no circle")
            mainClock.advanceTimeBy(400)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun aNonPositiveDurationIsRejected() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.ripple(duration = Duration.ZERO) }
    }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            ripple()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun keyFrame() =
        runEffectTest {
            ripple(ChangeEffect.ripple(shape = CircleShape, origin = TransformOrigin(0.3f, 0.3f)))
            value++
            mainClock.advanceTimeBy(200)
            onRoot().captureRoboImage("src/jvmTest/screenshots/ripple_200.png")
        }
}
