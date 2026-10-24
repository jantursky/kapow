package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TransitionTest {
    private var visible by mutableStateOf(false)
    private val counter = CompositionCounter()

    /** The element sits at 150..250 on both axes once it is shown. */
    private fun ComposeUiTest.scene(
        enter: KapowEnter = KapowEnter.None,
        exit: KapowExit = KapowExit.None,
        motion: KapowMotion = KapowMotion.Full,
        feedback: KapowFeedback? = null,
    ) {
        setEffectContent(motion) {
            CompositionLocalProvider(LocalKapowFeedback provides feedback) {
                KapowParticleLayer(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(Color.White)) {
                        KapowAnimatedVisibility(
                            visible,
                            Modifier.offset(150.dp, 150.dp),
                            enter,
                            exit,
                            feedback = KapowFeedbackEvent.Selection,
                        ) {
                            counter.Count()
                            SolidElement(Modifier)
                        }
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.show() {
        visible = true
        mainClock.advanceTimeByFrame()
    }

    private fun ComposeUiTest.shown(
        enter: KapowEnter = KapowEnter.None,
        exit: KapowExit = KapowExit.None,
        motion: KapowMotion = KapowMotion.Full,
    ) {
        visible = true
        scene(enter, exit, motion)
        mainClock.advanceTimeBy(2000)
    }

    private fun ImageBitmap.nonWhite(
        xs: Iterable<Int>,
        ys: Iterable<Int>,
    ): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in ys) for (x in xs) if (pixels[x, y] != Color.White) n++
        return n
    }

    @Test
    fun irisEnterGrowsFromTheCenter() =
        runEffectTest {
            scene(enter = KapowEnter.iris())
            show()
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(200, 200))
            assertEquals(Color.White, image.colorAt(152, 152))
            mainClock.advanceTimeBy(600)
            assertEquals(Cobalt, capture().colorAt(152, 152), "covers the corners at the end")
        }

    @Test
    fun irisExitShrinksAndTheContentStaysUntilItEnds() =
        runEffectTest {
            shown(exit = KapowExit.iris())
            visible = false
            mainClock.advanceTimeBy(250)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(200, 200))
            assertEquals(Color.White, image.colorAt(152, 152))
            mainClock.advanceTimeBy(600)
            assertEquals(Color.White, capture().colorAt(200, 200))
        }

    @Test
    fun clockSweepsClockwiseFromTwelve() =
        runEffectTest {
            scene(enter = KapowEnter.clock())
            show()
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(230, 165), "top right first")
            assertEquals(Color.White, image.colorAt(170, 165), "top left last")
        }

    @Test
    fun wipeRevealsAlongItsAngle() =
        runEffectTest {
            scene(enter = KapowEnter.wipe(angle = 0f))
            show()
            mainClock.advanceTimeBy(150)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(152, 200))
            assertEquals(Color.White, image.colorAt(248, 200))
        }

    @Test
    fun glareLightsItsEdge() =
        runEffectTest {
            scene(enter = KapowEnter.glare(angle = 0f, color = Color.White))
            show()
            mainClock.advanceTimeBy(100)
            val pixels = capture().toPixelMap()
            val brightest = (150 until 250).maxOf { pixels[it, 200].luminance }
            assertTrue(brightest > Cobalt.luminance + 0.1f, "a streak brighter than the element")
        }

    @Test
    fun blindsOpenEachSlatFromItsMiddle() =
        runEffectTest {
            scene(enter = KapowEnter.blinds(slatWidth = 20.dp))
            show()
            mainClock.advanceTimeBy(150)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(200, 160), "middle of the first slat")
            assertEquals(Color.White, image.colorAt(200, 151), "edge of the first slat")
            mainClock.advanceTimeBy(600)
            assertEquals(Cobalt, capture().colorAt(200, 151))
        }

    @Test
    fun poofHidesAtOnceAndLeavesAPuff() =
        runEffectTest {
            shown(exit = KapowExit.poof(Color.Black))
            visible = false
            mainClock.advanceTimeBy(50)
            val image = capture()
            assertNotEquals(Cobalt, image.colorAt(152, 152), "the content is gone")
            assertTrue(image.nonWhite(100 until 300, 100 until 300) > 0, "a puff where it was")
            mainClock.advanceTimeBy(1000)
            assertEquals(0, capture().nonWhite(0 until 400, 0 until 400))
        }

    @Test
    fun anvilFallsLandsAndKicksUpDust() =
        runEffectTest {
            scene(enter = KapowEnter.anvil())
            show()
            mainClock.advanceTimeBy(50)
            assertEquals(Color.White, capture().colorAt(200, 200), "still falling")
            mainClock.advanceTimeBy(250)
            val dust = capture().nonWhite((0 until 140) + (260 until 400), 200 until 270)
            assertTrue(dust > 0, "dust beside the element")
            mainClock.advanceTimeBy(2000)
            assertEquals(Cobalt, capture().colorAt(151, 151))
        }

    @Test
    fun popGrowsInWithARing() =
        runEffectTest {
            scene(enter = KapowEnter.pop(Color.Red))
            show()
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(200, 200))
            assertEquals(Color.White, image.colorAt(152, 152), "not grown yet")
            mainClock.advanceTimeBy(1500)
            assertEquals(Cobalt, capture().colorAt(151, 151))
        }

    @Test
    fun reducedMotionFadesInstead() =
        runEffectTest {
            scene(enter = KapowEnter.iris(), motion = KapowMotion.Reduced)
            show()
            mainClock.advanceTimeBy(150)
            val corner = capture().colorAt(152, 152)
            assertNotEquals(Color.White, corner, "the corner shows without a mask")
            assertNotEquals(Cobalt, corner, "faded")
        }

    @Test
    fun feedbackFiresAsContentEntersAndExits() =
        runEffectTest {
            val events = mutableListOf<KapowFeedbackEvent>()
            scene(enter = KapowEnter.iris(), exit = KapowExit.iris(), feedback = KapowFeedback { events += it })
            show()
            mainClock.advanceTimeBy(600)
            visible = false
            mainClock.advanceTimeBy(600)
            assertEquals(listOf<KapowFeedbackEvent>(KapowFeedbackEvent.Selection, KapowFeedbackEvent.Selection), events)
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene(enter = KapowEnter.iris())
            show()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            repeat(20) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1, "recomposed ${counter.count - before} times")
        }

    @Test
    fun keyFrames() =
        runEffectTest {
            scene(enter = KapowEnter.iris())
            show()
            mainClock.advanceTimeBy(150)
            onRoot().captureRoboImage("src/jvmTest/screenshots/iris_enter_150.png")
        }
}
