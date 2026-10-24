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
class StampTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        effect: ChangeEffect = ChangeEffect.stamp(scale = 1.4f, dustColor = Color.Red),
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            KapowParticleLayer(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    counter.Count()
                    SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, effect))
                }
            }
        }
    }

    /** Reddish dust along the base, beside the element. */
    private fun ImageBitmap.dust(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 225 until 260) {
            for (x in 100 until 300) {
                if (x in 148 until 252) continue
                val c = pixels[x, y]
                if (c.red > c.blue + 0.05f && c.green < 0.97f) n++
            }
        }
        return n
    }

    @Test
    fun theStampStartsLargeAndLandsWithDust() =
        runEffectTest {
            scene()
            value++
            mainClock.advanceTimeBy(40)
            assertTrue(capture().colorAt(140, 200) != Color.White, "larger than the element at first")
            mainClock.advanceTimeBy(200)
            assertTrue(capture().dust() > 0, "dust beside the base after landing")
            mainClock.advanceTimeBy(1200)
            val after = capture()
            assertEquals(0, after.dust())
            assertEquals(Color.White, after.colorAt(145, 200))
            assertEquals(Cobalt, after.colorAt(152, 200))
            assertEquals(Cobalt, after.colorAt(200, 248))
        }

    @Test
    fun theImpactKeepsTheBaseOnTheGround() =
        runEffectTest {
            scene()
            value++
            // Just after landing: squashed flat, so the top comes down while the base stays.
            mainClock.advanceTimeBy(190)
            val image = capture()
            assertEquals(Color.White, image.colorAt(200, 152), "the top has come down")
            assertTrue(image.colorAt(200, 248) != Color.White, "the base stays on the ground")
        }

    @Test
    fun reducedMotionBrightensWithoutMovingOrDust() =
        runEffectTest {
            scene(motion = KapowMotion.Reduced)
            val base = capture().colorAt(200, 200).luminance
            value++
            mainClock.advanceTimeBy(60)
            val image = capture()
            assertEquals(Color.White, image.colorAt(145, 200))
            assertEquals(0, image.dust())
            assertTrue(image.colorAt(200, 200).luminance > base + 0.05f)
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
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun parametersAreChecked() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.stamp(scale = 0.9f) }
    }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene(ChangeEffect.stamp())
            value++
            mainClock.advanceTimeBy(200)
            onRoot().captureRoboImage("src/jvmTest/screenshots/stamp_200.png")
        }
}
