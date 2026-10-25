package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ShimmerTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.shimmer()

    /** The brightest the element gets along its horizontal middle. */
    private fun androidx.compose.ui.graphics.ImageBitmap.peak(): Float =
        (150 until 250).maxOf {
            colorAt(it, 200).luminance
        }

    @Test
    fun aBandSweepsTheElementAndRestsBetweenSweeps() =
        runEffectTest {
            scene.show(this, effect)
            val before = capture()
            scene.on = true
            mainClock.advanceTimeBy(750)
            val mid = capture()
            assertTrue(mid.peak() > before.peak() + 0.02f, "a band of light mid-sweep")
            assertEquals(Color.White, mid.colorAt(149, 200), "nothing outside the element")
            mainClock.advanceTimeBy(950)
            assertEquals(0, capture().pixelsDifferentFrom(before), "resting between sweeps")
            mainClock.advanceTimeBy(750)
            assertTrue(capture().peak() > before.peak() + 0.02f, "the next sweep")
        }

    @Test
    fun itFadesOutAndLeavesTheElementUntouched() =
        runEffectTest {
            scene.show(this, effect)
            val before = capture()
            scene.on = true
            mainClock.advanceTimeBy(750)
            scene.on = false
            mainClock.advanceTimeBy(400)
            assertEquals(0, capture().pixelsDifferentFrom(before))
        }

    @Test
    fun reducedMotionLightensTheElementEvenly() =
        runEffectTest {
            scene.show(this, effect, KapowMotion.Reduced)
            val before = capture()
            scene.on = true
            mainClock.advanceTimeBy(300)
            val first = capture()
            assertTrue(first.colorAt(160, 160).luminance > before.colorAt(160, 160).luminance + 0.02f)
            assertEquals(first.colorAt(160, 160), first.colorAt(240, 240), "the same everywhere")
            mainClock.advanceTimeBy(700)
            assertEquals(0, capture().pixelsDifferentFrom(first), "nothing travels")
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
            scene.show(this, ConditionalEffect.shimmer(Color.White.copy(alpha = 0.6f)))
            scene.on = true
            mainClock.advanceTimeBy(750)
            onRoot().captureRoboImage("src/jvmTest/screenshots/shimmer_750.png")
        }
}
