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
class SparkleTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.sparkle(
        effect: ChangeEffect = ChangeEffect.sparkle(Color.Red),
        motion: KapowMotion = KapowMotion.Full,
        withLayer: Boolean = true,
    ) {
        setEffectContent(motion) {
            val content = @androidx.compose.runtime.Composable {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    counter.Count()
                    SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, effect))
                }
            }
            if (withLayer) KapowParticleLayer(Modifier.fillMaxSize()) { content() } else content()
        }
    }

    /** Reddish pixels anywhere, which only stars can be. */
    private fun ImageBitmap.stars(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 0 until 400) {
            for (x in 0 until 400) {
                val c = pixels[x, y]
                if (c.red > 0.5f && c.blue < 0.7f) n++
            }
        }
        return n
    }

    @Test
    fun starsTwinkleOverTheElementAndAreGone() =
        runEffectTest {
            sparkle()
            value++
            mainClock.advanceTimeBy(300)
            assertTrue(capture().stars() > 20, "stars over the element")
            mainClock.advanceTimeBy(900)
            assertEquals(0, capture().stars())
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun starsAppearOneAfterAnother() =
        runEffectTest {
            sparkle(ChangeEffect.sparkle(Color.Red, count = 12))
            value++
            mainClock.advanceTimeBy(50)
            val early = capture().stars()
            mainClock.advanceTimeBy(300)
            assertTrue(capture().stars() > early, "more stars later: $early")
        }

    @Test
    fun withoutAParticleLayerTheElementDrawsItsOwnStars() =
        runEffectTest {
            sparkle(withLayer = false)
            value++
            mainClock.advanceTimeBy(300)
            assertTrue(capture().stars() > 20)
        }

    @Test
    fun reducedMotionBrightensWithoutStars() =
        runEffectTest {
            sparkle(motion = KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(0, image.stars())
            assertTrue(image.colorAt(200, 200).luminance > Cobalt.luminance + 0.05f)
            mainClock.advanceTimeBy(500)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun aNonPositiveCountIsRejected() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.sparkle(count = 0) }
    }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            sparkle()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun keyFrame() =
        runEffectTest {
            sparkle(ChangeEffect.sparkle(count = 8))
            value++
            mainClock.advanceTimeBy(350)
            onRoot().captureRoboImage("src/jvmTest/screenshots/sparkle_350.png")
        }
}
