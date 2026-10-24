package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
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
class ProgressTraceTest {
    private var progress by mutableFloatStateOf(0f)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        effect: ProgressEffect = ProgressEffect.trace(color = Color.Red, width = 4.dp),
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
    fun theOutlineIsDrawnUpToTheProgress() =
        runEffectTest {
            scene()
            val empty = capture()
            for ((x, y) in listOf(220 to 152, 248 to 200, 152 to 200)) assertEquals(Cobalt, empty.colorAt(x, y))
            at(0.5f)
            val half = capture()
            assertTrue(half.colorAt(220, 152).isReddish(), "the top right")
            assertTrue(half.colorAt(248, 200).isReddish(), "the right edge")
            assertEquals(Cobalt, half.colorAt(152, 200), "not the left edge yet")
            assertEquals(Cobalt, half.colorAt(180, 152), "nor the top left")
            assertEquals(Cobalt, half.colorAt(200, 200))
            at(1f)
            val full = capture()
            for ((x, y) in listOf(220 to 152, 248 to 200, 200 to 248, 152 to 200, 180 to 152, 200 to 152)) {
                assertTrue(full.colorAt(x, y).isReddish(), "the closed outline at $x, $y")
            }
            assertEquals(Color.White, full.colorAt(146, 200), "inside the element's bounds")
        }

    @Test
    fun roundedShapesKeepTheirCornersClear() =
        runEffectTest {
            scene(ProgressEffect.trace(RoundedCornerShape(30.dp), Color.Red, 4.dp))
            at(1f)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(153, 153), "the corner is cut by the curve")
            assertTrue(image.colorAt(200, 152).isReddish())
        }

    @Test
    fun reducedMotionStillFollowsTheProgress() =
        runEffectTest {
            scene(motion = KapowMotion.Reduced)
            at(0.5f)
            assertTrue(capture().colorAt(248, 200).isReddish())
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
        assertFailsWith<IllegalArgumentException> { ProgressEffect.trace(width = 0.dp) }
    }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene(ProgressEffect.trace(RoundedCornerShape(16.dp)))
            at(0.6f)
            onRoot().captureRoboImage("src/jvmTest/screenshots/progress_trace_60.png")
        }
}
