package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class TraceTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        effect: ChangeEffect = ChangeEffect.trace(color = Color.Red, width = 4.dp, duration = 1.seconds),
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, effect))
            }
        }
    }

    private fun Color.isReddish() = red > 0.6f && blue < 0.6f

    /** Red pixels in the 6 px band just inside the element's edges, where the outline is drawn. */
    private fun ImageBitmap.redOnTheEdge(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 150 until 250) {
            for (x in 150 until 250) {
                val edge = x < 156 || x >= 244 || y < 156 || y >= 244
                if (edge && pixels[x, y].isReddish()) n++
            }
        }
        return n
    }

    /** Red pixels in the middle of the element, where the outline never goes. */
    private fun ImageBitmap.redInside(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 165 until 235) for (x in 165 until 235) if (pixels[x, y].isReddish()) n++
        return n
    }

    @Test
    fun aCometRunsAlongTheOutlineAndLeavesNothing() =
        runEffectTest {
            scene()
            value++
            mainClock.advanceTimeBy(400)
            val image = capture()
            assertTrue(image.redOnTheEdge() > 20, "a comet on the outline")
            assertEquals(0, image.redInside())
            assertEquals(Color.White, image.colorAt(145, 200), "the stroke stays inside the element")
            mainClock.advanceTimeBy(1000)
            assertEquals(0, capture().redOnTheEdge())
        }

    @Test
    fun theCometMovesClockwiseFromTheTop() =
        runEffectTest {
            scene()
            value++
            mainClock.advanceTimeBy(250)
            val early = capture()
            assertTrue(early.colorAt(220, 152).isReddish(), "on the top edge, right of the middle")
            assertTrue(!early.colorAt(152, 200).isReddish(), "not yet on the left edge")
        }

    @Test
    fun theCometFollowsTheShape() =
        runEffectTest {
            scene(ChangeEffect.trace(CircleShape, Color.Red, 4.dp, 1.seconds))
            value++
            repeat(40) {
                mainClock.advanceTimeByFrame()
                // The corners of the box stay clear of a circle's outline.
                assertTrue(!capture().colorAt(153, 153).isReddish())
            }
        }

    @Test
    fun reducedMotionShowsTheWholeOutlineAndFades() =
        runEffectTest {
            scene(motion = KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(50)
            val image = capture()
            for ((x, y) in listOf(200 to 152, 248 to 200, 200 to 248, 152 to 200)) {
                assertTrue(image.colorAt(x, y).isReddish(), "the outline at $x, $y")
            }
            mainClock.advanceTimeBy(600)
            assertEquals(0, capture().redOnTheEdge())
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
        assertFailsWith<IllegalArgumentException> { ChangeEffect.trace(width = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.trace(duration = Duration.ZERO) }
    }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene(ChangeEffect.trace())
            value++
            mainClock.advanceTimeBy(400)
            onRoot().captureRoboImage("src/jvmTest/screenshots/trace_400.png")
        }
}
