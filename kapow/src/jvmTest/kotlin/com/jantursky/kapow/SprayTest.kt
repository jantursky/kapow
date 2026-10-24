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
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SprayTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()
    private val red = ParticleContent.shape(ParticleShape.Square, listOf(Color.Red), 8.dp)

    private fun ComposeUiTest.scene(
        effect: ChangeEffect,
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

    /** Red pixels above the element, where only particles can be. */
    private fun ImageBitmap.particlesAbove(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 0 until 150) for (x in 0 until 400) if (pixels[x, y].isReddish()) n++
        return n
    }

    private fun Color.isReddish() = red > 0.6f && green < 0.6f && blue < 0.6f

    @Test
    fun aSprayThrowsParticlesUpThatDisappear() =
        runEffectTest {
            scene(ChangeEffect.spray(red))
            value++
            mainClock.advanceTimeBy(250)
            assertTrue(capture().particlesAbove() > 0, "particles above the element")
            mainClock.advanceTimeBy(1500)
            assertEquals(0, capture().particlesAbove())
        }

    @Test
    fun reducedMotionBrightensInsteadOfSpraying() =
        runEffectTest {
            scene(ChangeEffect.spray(red), KapowMotion.Reduced)
            val base = capture().colorAt(200, 200).luminance
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(0, image.particlesAbove())
            assertTrue(image.colorAt(200, 200).luminance > base + 0.05f)
            mainClock.advanceTimeBy(500)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun eachChangeRisesItsOwnLabel() =
        runEffectTest {
            scene(ChangeEffect.rise(red))
            value++
            mainClock.advanceTimeBy(400)
            val one = capture().particlesAbove()
            assertTrue(one > 0, "a label above the element")
            value++
            mainClock.advanceTimeBy(100)
            assertTrue(capture().particlesAbove() > one, "a second label joins the first")
            mainClock.advanceTimeBy(2000)
            assertEquals(0, capture().particlesAbove())
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene(ChangeEffect.spray())
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene(ChangeEffect.spray(count = 16))
            value++
            mainClock.advanceTimeBy(300)
            onRoot().captureRoboImage("src/jvmTest/screenshots/spray_300.png")
        }
}
