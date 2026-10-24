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
class FireworksTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    // The element sits low, so the rocket has room to climb above it.
    private fun ComposeUiTest.fireworks(
        effect: ChangeEffect = ChangeEffect.fireworks(listOf(Color.Red)),
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            KapowParticleLayer(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    counter.Count()
                    SolidElement(Modifier.offset(150.dp, 280.dp).changeEffect(value, effect))
                }
            }
        }
    }

    /** Painted pixels above the element whose distance from its center line is in [columns]. */
    private fun ImageBitmap.paintedAbove(columns: IntRange): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 0 until 280) {
            for (x in 0 until 400) {
                if (kotlin.math.abs(x - 200) in columns && pixels[x, y] != Color.White) n++
            }
        }
        return n
    }

    @Test
    fun aRocketClimbsThenBurstsIntoSparksThatAreGone() =
        runEffectTest {
            fireworks()
            value++
            mainClock.advanceTimeBy(250)
            val climbing = capture()
            assertTrue(climbing.paintedAbove(0..6) > 0, "the rocket above the element")
            assertEquals(0, climbing.paintedAbove(20..400), "nothing has burst yet")
            mainClock.advanceTimeBy(600)
            assertTrue(capture().paintedAbove(30..400) > 30, "sparks spread to both sides")
            mainClock.advanceTimeBy(1500)
            assertEquals(0, capture().paintedAbove(0..400))
        }

    @Test
    fun reducedMotionBrightensWithoutLaunching() =
        runEffectTest {
            fireworks(motion = KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(0, image.paintedAbove(0..400))
            assertTrue(image.colorAt(200, 330).luminance > Cobalt.luminance + 0.05f)
            mainClock.advanceTimeBy(500)
            assertEquals(Cobalt, capture().colorAt(200, 330))
        }

    @Test
    fun invalidSettingsAreRejected() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.fireworks(colors = emptyList()) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.fireworks(count = 0) }
    }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            fireworks()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(90) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun keyFrames() =
        runEffectTest {
            fireworks(ChangeEffect.fireworks())
            value++
            mainClock.advanceTimeBy(300)
            onRoot().captureRoboImage("src/jvmTest/screenshots/fireworks_300.png")
            mainClock.advanceTimeBy(450)
            onRoot().captureRoboImage("src/jvmTest/screenshots/fireworks_750.png")
        }
}
