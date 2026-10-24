package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * A white window with one cobalt 100 × 100 element at 150..250 on both axes, shown and hidden by
 * [KapowAnimatedVisibility] under a particle layer. The checks every transition shares live here.
 */
@OptIn(ExperimentalTestApi::class)
class TransitionScene(
    private val test: ComposeUiTest,
) {
    var visible by mutableStateOf(false)
    val counter = CompositionCounter()

    fun set(
        enter: KapowEnter = KapowEnter.None,
        exit: KapowExit = KapowExit.None,
        motion: KapowMotion = KapowMotion.Full,
    ) {
        test.setEffectContent(motion) {
            KapowParticleLayer(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    KapowAnimatedVisibility(visible, Modifier.offset(150.dp, 150.dp), enter, exit) {
                        counter.Count()
                        SolidElement(Modifier)
                    }
                }
            }
        }
    }

    fun show() {
        visible = true
        test.mainClock.advanceTimeByFrame()
    }

    /** Sets the scene up already shown, settled, then starts hiding it. */
    fun hideFromShown(
        exit: KapowExit,
        motion: KapowMotion = KapowMotion.Full,
    ) {
        visible = true
        set(exit = exit, motion = motion)
        test.mainClock.advanceTimeBy(2000)
        visible = false
        test.mainClock.advanceTimeByFrame()
    }

    fun advance(millis: Long) = test.mainClock.advanceTimeBy(millis)

    fun capture(): ImageBitmap = test.capture()

    /** The share of the element's pixels that are cobalt now, from 0 to 1. */
    fun shown(image: ImageBitmap = capture()): Float {
        val pixels = image.toPixelMap()
        var n = 0
        for (y in 150 until 250) for (x in 150 until 250) if (pixels[x, y] == Cobalt) n++
        return n / 10_000f
    }

    /** Pixels in the window that are neither white nor the element's cobalt. */
    fun others(image: ImageBitmap = capture()): Int {
        val pixels = image.toPixelMap()
        var n = 0
        for (y in 0 until 400) for (x in 0 until 400) if (pixels[x, y] != Color.White && pixels[x, y] != Cobalt) n++
        return n
    }

    /** Checks that [enter] shows part of the element at [midMillis] and all of it once it ends. */
    fun checkEnter(
        enter: KapowEnter,
        midMillis: Long,
    ) {
        set(enter = enter)
        show()
        advance(midMillis)
        val mid = shown()
        assertTrue(mid > 0.05f && mid < 0.95f, "part of the element shows mid-way, not $mid")
        advance(3000)
        assertEquals(1f, shown(), "all of it shows at the end")
    }

    /** Checks that [exit] hides part of the element at [midMillis] and all of it, with nothing left, at the end. */
    fun checkExit(
        exit: KapowExit,
        midMillis: Long,
    ) {
        hideFromShown(exit)
        advance(midMillis)
        val mid = shown()
        assertTrue(mid > 0.05f && mid < 0.95f, "part of the element shows mid-way, not $mid")
        advance(3000)
        val image = capture()
        assertEquals(0f, shown(image), "gone at the end")
        assertEquals(0, others(image), "nothing left behind")
    }

    /** Checks that with reduced motion [enter] and [exit] fade the whole element instead. */
    fun checkReducedFades(
        enter: KapowEnter,
        exit: KapowExit,
    ) {
        set(enter = enter, exit = exit, motion = KapowMotion.Reduced)
        show()
        advance(150)
        for ((x, y) in listOf(151 to 151, 200 to 200, 248 to 248)) {
            val color = capture().colorAt(x, y)
            assertNotEquals(Color.White, color, "($x, $y) shows without a mask")
            assertNotEquals(Cobalt, color, "($x, $y) is faded")
        }
        advance(3000)
        assertEquals(1f, shown())
        visible = false
        test.mainClock.advanceTimeByFrame()
        advance(150)
        assertNotEquals(Color.White, capture().colorAt(151, 151), "fading out, not cut")
        advance(3000)
        assertEquals(0f, shown())
    }

    /** Checks that with reduced motion an exit-only [exit] fades the whole element out instead. */
    fun checkReducedExitFades(exit: KapowExit) {
        hideFromShown(exit, KapowMotion.Reduced)
        advance(150)
        val image = capture()
        for ((x, y) in listOf(151 to 151, 200 to 200, 248 to 248)) {
            assertNotEquals(Color.White, image.colorAt(x, y), "($x, $y) is still there")
            assertNotEquals(Cobalt, image.colorAt(x, y), "($x, $y) is faded")
        }
        assertEquals(100 * 100, others(image), "the whole element is faded and nothing else is drawn")
        advance(3000)
        assertEquals(0f, shown())
    }

    /** Checks that animation frames of [enter] and [exit] redraw without recomposing the content. */
    fun checkFramesDoNotRecompose(
        enter: KapowEnter,
        exit: KapowExit,
    ) {
        set(enter = enter, exit = exit)
        show()
        test.mainClock.advanceTimeByFrame()
        var before = counter.count
        repeat(20) { test.mainClock.advanceTimeByFrame() }
        assertTrue(counter.count - before <= 1, "entering recomposed ${counter.count - before} times")
        advance(3000)
        visible = false
        test.mainClock.advanceTimeByFrame()
        test.mainClock.advanceTimeByFrame()
        before = counter.count
        repeat(20) { test.mainClock.advanceTimeByFrame() }
        assertTrue(counter.count - before <= 1, "exiting recomposed ${counter.count - before} times")
    }

    /** Records the window at [millis] into the enter or exit as a Roborazzi key frame called [name]. */
    fun keyFrame(
        name: String,
        enter: KapowEnter? = null,
        exit: KapowExit? = null,
        millis: Long,
    ) {
        if (enter != null) {
            set(enter = enter)
            show()
        } else {
            hideFromShown(checkNotNull(exit))
        }
        advance(millis)
        test.onRoot().captureRoboImage("src/jvmTest/screenshots/$name.png")
    }
}

/** Runs [block] on a fresh [TransitionScene]. */
@OptIn(ExperimentalTestApi::class)
fun runTransitionTest(block: TransitionScene.() -> Unit) = runEffectTest { TransitionScene(this).block() }
