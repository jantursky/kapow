package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration

@OptIn(ExperimentalTestApi::class)
class ColorFlashTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    /** A 100 × 100 element whose left half is cobalt and whose right half is transparent. */
    private fun ComposeUiTest.flash(motion: KapowMotion = KapowMotion.Full) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                Box(Modifier.offset(150.dp, 150.dp).changeEffect(value, ChangeEffect.flash(Color.Red)).size(100.dp)) {
                    Box(Modifier.width(50.dp).fillMaxHeight().background(Cobalt))
                }
            }
        }
    }

    @Test
    fun theElementsPixelsFlashAndFadeBack() =
        runEffectTest {
            flash()
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            val tinted = image.colorAt(170, 200)
            assertTrue(tinted.red > 0.6f && tinted.blue < 0.6f, "red over the element: $tinted")
            assertEquals(Color.White, image.colorAt(230, 200), "the transparent half stays clear")
            mainClock.advanceTimeBy(500)
            assertEquals(Cobalt, capture().colorAt(170, 200))
        }

    @Test
    fun reducedMotionFlashesSofter() =
        runEffectTest {
            flash(KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            val tinted = capture().colorAt(170, 200)
            assertTrue(tinted.red > Cobalt.red + 0.1f, "still tinted: $tinted")
            assertTrue(tinted.blue > 0.5f, "but the element shows through: $tinted")
            mainClock.advanceTimeBy(500)
            assertEquals(Cobalt, capture().colorAt(170, 200))
        }

    @Test
    fun aNonPositiveDurationIsRejected() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.flash(duration = Duration.ZERO) }
    }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            flash()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(40) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun keyFrame() =
        runEffectTest {
            flash()
            value++
            mainClock.advanceTimeBy(100)
            onRoot().captureRoboImage("src/jvmTest/screenshots/flash_100.png")
        }
}
