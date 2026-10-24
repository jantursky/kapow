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
class ConfettiPopTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        effect: ChangeEffect = ChangeEffect.confettiPop(listOf(Color.Red)),
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

    /** Red pixels outside the element, where only confetti can be. */
    private fun ImageBitmap.confettiOutside(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 0 until 400) {
            for (x in 0 until 400) {
                if (x in 150 until 250 && y in 150 until 250) continue
                val c = pixels[x, y]
                if (c.red > 0.6f && c.green < 0.6f && c.blue < 0.6f) n++
            }
        }
        return n
    }

    @Test
    fun aPopThrowsConfettiThatFallsAway() =
        runEffectTest {
            scene()
            value++
            mainClock.advanceTimeBy(250)
            assertTrue(capture().confettiOutside() > 20, "confetti around the element")
            mainClock.advanceTimeBy(2000)
            assertEquals(0, capture().confettiOutside())
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun confettiUsesEveryColor() =
        runEffectTest {
            scene(ChangeEffect.confettiPop(listOf(Color.Red, Color.Green), count = 30))
            value++
            mainClock.advanceTimeBy(250)
            val pixels = capture().toPixelMap()
            var green = 0
            for (y in 0 until 150) {
                for (x in 0 until 400) {
                    if (pixels[x, y].let {
                            it.green > 0.4f && it.red < 0.3f
                        }
                    ) {
                        green++
                    }
                }
            }
            assertTrue(green > 0, "green pieces too")
        }

    @Test
    fun reducedMotionBrightensInsteadOfThrowingConfetti() =
        runEffectTest {
            scene(motion = KapowMotion.Reduced)
            val base = capture().colorAt(200, 200).luminance
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(0, image.confettiOutside())
            assertTrue(image.colorAt(200, 200).luminance > base + 0.05f)
            mainClock.advanceTimeBy(500)
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
        assertFailsWith<IllegalArgumentException> { ChangeEffect.confettiPop(count = 0) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.confettiPop(colors = emptyList()) }
    }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene(ChangeEffect.confettiPop())
            value++
            mainClock.advanceTimeBy(300)
            onRoot().captureRoboImage("src/jvmTest/screenshots/confetti_pop_300.png")
        }
}
