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
class HeartsTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        effect: ChangeEffect = ChangeEffect.hearts(Color.Red),
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

    private fun Color.isRed() = red > 0.6f && green < 0.5f && blue < 0.5f

    private fun ImageBitmap.redOutside(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 0 until 400) {
            for (x in 0 until 400) {
                if (x in 150 until 250 && y in 150 until 250) continue
                if (pixels[x, y].isRed()) n++
            }
        }
        return n
    }

    @Test
    fun theBigHeartPopsOverTheElementAndFades() =
        runEffectTest {
            scene(ChangeEffect.hearts(Color.Red, count = 0))
            assertEquals(Cobalt, capture().colorAt(200, 200))
            value++
            mainClock.advanceTimeBy(150)
            assertTrue(capture().colorAt(200, 195).isRed(), "heart over the element")
            mainClock.advanceTimeBy(1800)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun smallHeartsFloatUpAboveTheElement() =
        runEffectTest {
            scene(ChangeEffect.hearts(Color.Red, count = 8, bigHeart = false))
            value++
            mainClock.advanceTimeBy(500)
            val pixels = capture().toPixelMap()
            var above = 0
            for (y in 0 until 150) for (x in 0 until 400) if (pixels[x, y].isRed()) above++
            assertTrue(above > 20, "hearts above the element")
            mainClock.advanceTimeBy(2500)
            assertEquals(0, capture().redOutside())
        }

    @Test
    fun noSmallHeartsWithACountOfZero() =
        runEffectTest {
            scene(ChangeEffect.hearts(Color.Red, count = 0))
            value++
            mainClock.advanceTimeBy(500)
            assertEquals(0, capture().redOutside())
        }

    @Test
    fun reducedMotionFadesOneHeartInPlace() =
        runEffectTest {
            scene(motion = KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(300)
            val image = capture()
            assertTrue(image.colorAt(200, 195).isRed(), "one heart over the element")
            assertEquals(0, image.redOutside(), "no small hearts")
            mainClock.advanceTimeBy(1000)
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
        assertFailsWith<IllegalArgumentException> { ChangeEffect.hearts(count = -1) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.hearts(bigHeartScale = 0f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.hearts(overshoot = -1f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.hearts(heartSize = 0.dp) }
    }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene(ChangeEffect.hearts())
            value++
            mainClock.advanceTimeBy(400)
            onRoot().captureRoboImage("src/jvmTest/screenshots/hearts_400.png")
        }
}
