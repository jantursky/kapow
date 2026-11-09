package com.jantursky.kapow

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.transitions.canBlur
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class ExtraTransitionSceneTest {
    private val enters =
        mapOf(
            "boing" to KapowEnter.boing(),
            "skid" to KapowEnter.skid(),
            "rotate3D" to KapowEnter.rotate3D(),
            "blur" to KapowEnter.blur(),
            "flicker" to KapowEnter.flicker(),
        )
    private val exits =
        mapOf(
            "boing" to KapowExit.boing(),
            "skid" to KapowExit.skid(),
            "rotate3D" to KapowExit.rotate3D(),
            "blur" to KapowExit.blur(),
            "flicker" to KapowExit.flicker(),
        )

    /** Pixels of the window that are not white. */
    private fun ImageBitmap.inked(): Int {
        var n = 0
        for (y in 0 until 400) for (x in 0 until 400) if (colorAt(x, y) != Color.White) n++
        return n
    }

    @Test
    fun everyEnterIsHiddenAtFirstAndEndsFullyShown() {
        for ((name, enter) in enters) {
            runTransitionTest {
                set(enter = enter)
                assertEquals(0, capture().inked(), "$name hidden before it starts")
                show()
                var seen = false
                repeat(40) {
                    advance(25)
                    seen = seen || capture().inked() > 0
                }
                assertTrue(seen, "$name shows something while it plays")
                advance(3000)
                val image = capture()
                assertEquals(1f, shown(image), "$name ends fully shown")
                assertEquals(0, others(image), "$name leaves nothing else")
            }
        }
    }

    @Test
    fun everyExitEndsHiddenAndTheContainerKeepsTheContentUntilThen() {
        for ((name, exit) in exits) {
            runTransitionTest {
                hideFromShown(exit)
                advance(100)
                assertTrue(capture().inked() > 0, "$name is still there $name")
                advance(3000)
                val image = capture()
                assertEquals(0f, shown(image), "$name ends hidden")
                assertEquals(0, others(image), "$name leaves nothing behind")
            }
        }
    }

    @Test
    fun boingPassesFullSizeThenSettles() =
        runTransitionTest {
            set(enter = KapowEnter.boing(overshoot = 2f))
            show()
            var widest = 0
            repeat(50) {
                advance(14)
                val image = capture()
                widest = maxOf(widest, (0 until 400).count { image.colorAt(it, 200) != Color.White })
            }
            assertTrue(widest > 105, "grew past its 100 px: $widest")
            advance(3000)
            assertEquals(100, (0 until 400).count { capture().colorAt(it, 200) != Color.White })
        }

    @Test
    fun skidComesFromTheEdgePastItsPlaceAndSettles() =
        runTransitionTest {
            set(enter = KapowEnter.skid(SkidEdge.Left, overshoot = 2f, lean = 0f))
            show()
            var leftmost = 400
            var rightmost = 0
            repeat(60) {
                advance(10)
                val image = capture()
                val xs = (0 until 400).filter { image.colorAt(it, 200) != Color.White }
                if (xs.isNotEmpty()) {
                    leftmost = minOf(leftmost, xs.min())
                    rightmost = maxOf(rightmost, xs.max())
                }
            }
            assertTrue(leftmost < 140, "starts to the left of its place: $leftmost")
            assertTrue(rightmost > 250, "passes its place to the right: $rightmost")
            advance(3000)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(152, 200))
            assertEquals(Color.White, image.colorAt(148, 200))
            assertEquals(Color.White, image.colorAt(252, 200))
        }

    @Test
    fun skidLeavesAcrossTheOppositeEdge() =
        runTransitionTest {
            hideFromShown(KapowExit.skid(SkidEdge.Right, lean = 0f, overshoot = 0f))
            var rightmost = 0
            repeat(40) {
                advance(15)
                val xs = (0 until 400).filter { capture().colorAt(it, 200) != Color.White }
                if (xs.isNotEmpty()) rightmost = maxOf(rightmost, xs.max())
            }
            assertTrue(rightmost > 280, "slides toward the right edge: $rightmost")
            advance(3000)
            assertEquals(0, capture().inked())
        }

    @Test
    fun rotate3DNarrowsTheElementAndDarkensIt() =
        runTransitionTest {
            set(enter = KapowEnter.rotate3D(SpinAxis.Y, degrees = 80f))
            show()
            advance(80)
            val image = capture()
            assertEquals(Color.White, image.colorAt(152, 200), "the near edge has left its place")
            val inked = (0 until 400).count { image.colorAt(it, 200) != Color.White }
            assertTrue(inked in 1..99, "narrower than its width: $inked")
            val xs = (0 until 400).filter { image.colorAt(it, 200) != Color.White }
            val middle = image.colorAt(xs[xs.size / 2], 200)
            assertTrue(middle.luminance < Cobalt.luminance, "darkened while turned away")
        }

    @Test
    fun blurAvailableBlursAndFadesIn() =
        runTransitionTest {
            val before = canBlur
            canBlur = { true }
            try {
                set(enter = KapowEnter.blur(radius = 20.dp))
                show()
                advance(100)
                val image = capture()
                // A blur smears the edge outward, so white around the edge turns tinted.
                assertNotEquals(Color.White, image.colorAt(146, 200), "the blur spreads past the edge")
                assertNotEquals(Cobalt, image.colorAt(200, 200), "faded")
                advance(3000)
                assertEquals(1f, shown())
                assertEquals(Color.White, capture().colorAt(146, 200))
            } finally {
                canBlur = before
            }
        }

    @Test
    fun blurUnavailableFadesAndScalesWithoutBlur() =
        runTransitionTest {
            val before = canBlur
            canBlur = { false }
            try {
                set(enter = KapowEnter.blur(radius = 20.dp, scale = 0.5f))
                show()
                advance(100)
                val image = capture()
                assertEquals(Color.White, image.colorAt(146, 200), "no blur past the edge")
                assertEquals(Color.White, image.colorAt(152, 200), "scaled down, so the edge is pulled in")
                assertNotEquals(Color.White, image.colorAt(200, 200))
                advance(3000)
                assertEquals(1f, shown())
            } finally {
                canBlur = before
            }
        }

    @Test
    fun flickerBlinksThenStaysOn() =
        runTransitionTest {
            set(enter = KapowEnter.flicker(duration = 1200.milliseconds, steps = 8))
            show()
            val states = mutableListOf<Boolean>()
            repeat(60) {
                advance(20)
                states += capture().inked() > 0
            }
            assertTrue(states.any { it } && states.any { !it }, "on and off both occur: $states")
            assertTrue(states.dropWhile { !it }.zipWithNext().count { (a, b) -> a != b } >= 1, "switches")
            advance(3000)
            assertEquals(1f, shown())
        }

    @Test
    fun aDimmedFlickerNeverGoesFullyDark() =
        runTransitionTest {
            set(enter = KapowEnter.flicker(duration = 1200.milliseconds, dim = 0.4f))
            show()
            repeat(60) {
                advance(20)
                if (it > 1) assertTrue(capture().inked() > 0, "frame $it shows the dimmed content")
            }
        }

    @Test
    fun aDelayKeepsAnEnterHiddenAndAnExitShown() {
        for ((name, enter) in enters) {
            runTransitionTest {
                set(enter = enter.delay(500.milliseconds))
                show()
                advance(400)
                assertEquals(0, capture().inked(), "$name enter is hidden during its delay")
                advance(3000)
                assertEquals(1f, shown(), "$name enter plays after it")
            }
        }
        for ((name, exit) in exits) {
            runTransitionTest {
                hideFromShown(exit.delay(500.milliseconds))
                advance(400)
                assertEquals(1f, shown(), "$name exit is fully shown during its delay")
                advance(3000)
                assertEquals(0f, shown(), "$name exit plays after it")
            }
        }
    }

    @Test
    fun easingShapesTheProgress() {
        // Eased in, the start is slower than on a linear clock.
        val slow = ExtraEasingProbe.inkAt(KapowEnter.rotate3D().easing(FastOutSlowInEasing), 100)
        val even = ExtraEasingProbe.inkAt(KapowEnter.rotate3D().easing(LinearEasing), 100)
        assertNotEquals(slow, even)
    }

    @Test
    fun reducedMotionFadesEveryOne() {
        for (name in enters.keys) {
            runTransitionTest { checkReducedFades(enters.getValue(name), exits.getValue(name)) }
        }
    }

    @Test
    fun theFadeWithReducedMotionIsNoLongerThanTheTransition() =
        runTransitionTest {
            set(enter = KapowEnter.blur(duration = 300.milliseconds), motion = KapowMotion.Reduced)
            show()
            advance(350)
            assertEquals(1f, shown())
        }

    @Test
    fun theEndStateOptionShowsTheContentAtOnce() =
        runTransitionTest {
            set(enter = KapowEnter.boing().reducedMotion(ReducedMotionMode.EndState), motion = KapowMotion.Reduced)
            show()
            advance(32)
            assertEquals(1f, shown())
        }

    @Test
    fun feedbackIsSentOncePerEnterAndExit() {
        for (name in enters.keys) {
            val events = mutableListOf<KapowFeedbackEvent>()
            var visible by mutableStateOf(false)
            runEffectTest {
                setEffectContent {
                    CompositionLocalProvider(LocalKapowFeedback provides KapowFeedback { events += it }) {
                        Box(Modifier.fillMaxSize().background(Color.White)) {
                            KapowAnimatedVisibility(
                                visible,
                                Modifier.offset(150.dp, 150.dp),
                                enters.getValue(name),
                                exits.getValue(name),
                                feedback = KapowFeedbackEvent.Selection,
                            ) { SolidElement(Modifier) }
                        }
                    }
                }
                visible = true
                mainClock.advanceTimeBy(3000)
                visible = false
                mainClock.advanceTimeBy(3000)
            }
            assertEquals(2, events.size, "$name: $events")
        }
    }

    @Test
    fun framesDoNotRecompose() {
        for (name in enters.keys) {
            runTransitionTest { checkFramesDoNotRecompose(enters.getValue(name), exits.getValue(name)) }
        }
    }

    @Test
    fun keyFrames() {
        runTransitionTest { keyFrame("boing_enter_90", enter = KapowEnter.boing(squash = 1f), millis = 90) }
        runTransitionTest { keyFrame("boing_exit_120", exit = KapowExit.boing(squash = 1f), millis = 120) }
        runTransitionTest { keyFrame("skid_left_enter_150", enter = KapowEnter.skid(), millis = 150) }
        runTransitionTest { keyFrame("skid_right_exit_200", exit = KapowExit.skid(), millis = 200) }
        runTransitionTest { keyFrame("rotate3d_y_enter_100", enter = KapowEnter.rotate3D(), millis = 100) }
        runTransitionTest { keyFrame("blur_enter_150", enter = KapowEnter.blur(radius = 12.dp), millis = 150) }
        runTransitionTest { keyFrame("blur_exit_150", exit = KapowExit.blur(radius = 12.dp), millis = 150) }
        runTransitionTest { keyFrame("flicker_enter_400", enter = KapowEnter.flicker(), millis = 400) }
        runTransitionTest { keyFrame("rotate3d_x_exit_150", exit = KapowExit.rotate3D(SpinAxis.X), millis = 150) }
    }
}

/** Runs a single enter and reports how many pixels of the window it has inked after [millis]. */
@OptIn(ExperimentalTestApi::class)
private object ExtraEasingProbe {
    fun inkAt(
        enter: KapowEnter,
        millis: Long,
    ): Int {
        var n = 0
        runTransitionTest {
            set(enter = enter)
            show()
            advance(millis)
            val image = capture()
            for (y in 0 until 400) for (x in 0 until 400) if (image.colorAt(x, y) != Color.White) n++
        }
        return n
    }
}
