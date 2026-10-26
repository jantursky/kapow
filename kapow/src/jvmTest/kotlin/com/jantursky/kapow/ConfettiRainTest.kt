package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ConfettiRainTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.confettiRain(listOf(Color.Red, Color.Green), density = 60f)

    /** Pixels that are not white and not inside the element, where only confetti can be. */
    private fun ImageBitmap.confetti(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x in 150 until 250 && y in 150 until 250) continue
                if (pixels[x, y] != Color.White) n++
            }
        }
        return n
    }

    @Test
    fun confettiFallsPastTheElementWhileOnAndClearsAfter() =
        runEffectTest {
            scene.show(this, effect)
            assertEquals(0, capture().confetti())
            scene.on = true
            mainClock.advanceTimeBy(2000)
            assertTrue(capture().confetti() > 0, "confetti around the element")
            scene.on = false
            mainClock.advanceTimeBy(4000)
            assertEquals(0, capture().confetti(), "the rest fell out and expired")
        }

    @Test
    fun noNewPiecesStartOnceOff() =
        runEffectTest {
            scene.show(this, effect)
            scene.on = true
            mainClock.advanceTimeBy(1000)
            scene.on = false
            // The longest piece lives 2.8 s, so by 3 s after switching off nothing is left.
            mainClock.advanceTimeBy(3000)
            assertEquals(0, capture().confetti())
        }

    @Test
    fun aVeryHighDensityStillRunsAndStopsCleanly() =
        runEffectTest {
            scene.show(this, ConditionalEffect.confettiRain(density = 5000f))
            scene.on = true
            mainClock.advanceTimeBy(1500)
            assertTrue(capture().confetti() > 0)
            scene.on = false
            mainClock.advanceTimeBy(4000)
            assertEquals(0, capture().confetti())
        }

    @Test
    fun reducedMotionDropsNone() =
        runEffectTest {
            scene.show(this, effect, KapowMotion.Reduced)
            scene.on = true
            mainClock.advanceTimeBy(2000)
            assertEquals(0, capture().confetti())
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
            scene.show(this, ConditionalEffect.confettiRain(density = 40f))
            scene.on = true
            mainClock.advanceTimeBy(1800)
            onRoot().captureRoboImage("src/jvmTest/screenshots/confetti_rain_1800.png")
        }
}
