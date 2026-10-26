package com.jantursky.kapow

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PingTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.ping(Color.Red)

    @Test
    fun ringsGrowOutOfTheElementWhileOn() =
        runEffectTest {
            scene.show(this, effect)
            assertEquals(0, capture().nonWhiteAbove())
            scene.on = true
            mainClock.advanceTimeBy(800)
            assertTrue(capture().nonWhiteAbove() > 0, "a ring around the element")
        }

    @Test
    fun ringsInFlightFinishAndNoNewOneStarts() =
        runEffectTest {
            scene.show(this, effect)
            scene.on = true
            mainClock.advanceTimeBy(1000)
            scene.on = false
            mainClock.advanceTimeBy(1000)
            assertTrue(capture().nonWhiteAbove() > 0, "the ring born at 0.8 s is still out")
            mainClock.advanceTimeBy(1800)
            assertEquals(0, capture().nonWhiteAbove(), "all gone, none started again")
        }

    @Test
    fun theRingsFollowTheShape() =
        runEffectTest {
            scene.show(this, ConditionalEffect.ping(Color.Red, shape = CircleShape, rings = 1))
            scene.on = true
            mainClock.advanceTimeBy(800)
            val image = capture()
            // A circle's ring leaves the corners of its square bounding box empty.
            assertEquals(Color.White, image.colorAt(125, 125))
            assertTrue(image.nonWhiteAbove() > 0)
        }

    @Test
    fun reducedMotionDrawsOneStaticFaintRing() =
        runEffectTest {
            scene.show(this, effect, KapowMotion.Reduced)
            scene.on = true
            mainClock.advanceTimeBy(300)
            val first = capture()
            assertTrue(first.nonWhiteAbove() > 0, "a ring")
            mainClock.advanceTimeBy(700)
            assertEquals(0, capture().pixelsDifferentFrom(first), "it stays still")
            scene.on = false
            mainClock.advanceTimeBy(300)
            assertEquals(0, capture().nonWhiteAbove())
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene.show(this, effect)
            assertTrue(scene.compositionsWhileOn(this) <= 1)
        }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene.show(
                this,
                ConditionalEffect.ping(
                    Color(0xFF6B62F2),
                    width = 3.dp,
                ),
            )
            scene.on = true
            mainClock.advanceTimeBy(1100)
            onRoot().captureRoboImage("src/jvmTest/screenshots/ping_1100.png")
        }
}
