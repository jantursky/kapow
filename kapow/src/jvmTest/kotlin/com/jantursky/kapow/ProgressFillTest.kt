package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

@OptIn(ExperimentalTestApi::class)
class ProgressFillTest {
    private var progress by mutableFloatStateOf(0f)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        effect: ProgressEffect = ProgressEffect.fill(Color.Red),
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(Modifier.offset(150.dp, 150.dp).progressEffect(effect) { progress })
            }
        }
        mainClock.advanceTimeByFrame()
    }

    private fun ComposeUiTest.at(value: Float) {
        progress = value
        mainClock.advanceTimeByFrame()
    }

    private fun Color.isReddish() = red > 0.6f && blue < 0.6f

    @Test
    fun theLiquidRisesWithProgress() =
        runEffectTest {
            scene()
            assertEquals(Cobalt, capture().colorAt(200, 245), "empty at 0")
            at(0.5f)
            val half = capture()
            assertTrue(half.colorAt(200, 245).isReddish(), "filled at the bottom")
            assertEquals(Cobalt, half.colorAt(200, 160), "not yet at the top")
            at(1f)
            val full = capture()
            assertTrue(full.colorAt(200, 152).isReddish(), "full at 1")
            assertEquals(Color.White, full.colorAt(200, 145), "only over the element's pixels")
        }

    @Test
    fun aTranslucentColorTintsTheElement() =
        runEffectTest {
            scene(ProgressEffect.fill(Color.Red.copy(alpha = 0.5f), wave = 0.dp))
            at(1f)
            val c = capture().colorAt(200, 200)
            assertTrue(c.red > 0.4f && c.blue > 0.3f, "a mix of the element and the tint: $c")
        }

    @Test
    fun reducedMotionStillFollowsTheProgress() =
        runEffectTest {
            scene(motion = KapowMotion.Reduced)
            at(0.5f)
            assertTrue(capture().colorAt(200, 245).isReddish())
        }

    @Test
    fun progressChangesDoNotRecompose() =
        runEffectTest {
            scene()
            val before = counter.count
            for (i in 1..20) at(i / 20f)
            assertEquals(before, counter.count)
        }

    @Test
    fun parametersAreChecked() {
        assertFailsWith<IllegalArgumentException> { ProgressEffect.fill(wave = (-1).dp) }
    }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene(ProgressEffect.fill(Color(0x99FFB547)))
            at(0.5f)
            onRoot().captureRoboImage("src/jvmTest/screenshots/progress_fill_50.png")
        }
}
