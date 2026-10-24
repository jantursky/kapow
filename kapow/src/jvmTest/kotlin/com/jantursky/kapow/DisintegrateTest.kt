package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.transitions.dustColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DisintegrateTest {
    /** Pixels above the element where [test] holds for the color. */
    private fun TransitionScene.dustAbove(test: (Color) -> Boolean): Int {
        val pixels = capture().toPixelMap()
        var n = 0
        for (y in 0 until 148) for (x in 0 until 400) if (pixels[x, y] != Color.White && test(pixels[x, y])) n++
        return n
    }

    @Test
    fun exitsAsDust() = runTransitionTest { checkExit(KapowExit.disintegrate(), midMillis = 500) }

    @Test
    fun sweepsFromLeftToRight() =
        runTransitionTest {
            hideFromShown(KapowExit.disintegrate())
            advance(450)
            val pixels = capture().toPixelMap()
            var left = 0
            var right = 0
            for (y in 150 until 250) {
                for (x in 0 until 25) {
                    if (pixels[150 + x, y] == Cobalt) left++
                    if (pixels[225 + x, y] == Cobalt) right++
                }
            }
            assertTrue(left < right / 2, "$left px left, $right px right")
            assertTrue(right > 2400, "the right edge is untouched but for a little dust, $right px")
        }

    @Test
    fun theDustTakesTheContentsColor() =
        runTransitionTest {
            hideFromShown(KapowExit.disintegrate())
            advance(700)
            val blue = dustAbove { it.blue > it.red + 0.2f }
            assertTrue(blue > 0, "cobalt dust above the element")
            assertEquals(0, dustAbove { it.red > it.blue + 0.2f }, "and nothing else")
        }

    @Test
    fun aGivenColorIsUsedInstead() =
        runTransitionTest {
            hideFromShown(KapowExit.disintegrate(color = Color.Red))
            advance(700)
            assertTrue(dustAbove { it.red > it.blue + 0.2f } > 0, "red dust above the element")
        }

    @Test
    fun showingAgainMidWayStopsTheDust() =
        runTransitionTest {
            set(exit = KapowExit.disintegrate())
            visible = true
            advance(2000)
            visible = false
            advance(300)
            visible = true
            advance(3000)
            assertEquals(1f, shown())
            assertEquals(0, others())
            visible = false
            advance(3000)
            assertEquals(0f, shown(), "a second exit runs to the end")
        }

    @Test
    fun leavingTheTreeMidWayStopsTheDustAndStartsCleanLater() =
        runEffectTest {
            var present by mutableStateOf(true)
            var visible by mutableStateOf(true)
            setEffectContent {
                KapowParticleLayer(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(Color.White)) {
                        if (present) {
                            KapowAnimatedVisibility(
                                visible,
                                Modifier.offset(150.dp, 150.dp),
                                exit = KapowExit.disintegrate(),
                            ) {
                                SolidElement(Modifier)
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            visible = false
            mainClock.advanceTimeBy(300)
            present = false
            mainClock.advanceTimeBy(100)
            visible = true
            present = true
            mainClock.advanceTimeBy(3000)
            assertEquals(Cobalt, capture().colorAt(151, 151), "shown again, whole")
            visible = false
            mainClock.advanceTimeBy(3000)
            assertEquals(Color.White, capture().colorAt(200, 200), "and the next exit runs to the end")
        }

    @Test
    fun aNewExitMidWayTakesOver() =
        runEffectTest {
            var exit by mutableStateOf(KapowExit.disintegrate())
            var visible by mutableStateOf(true)
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    KapowAnimatedVisibility(visible, Modifier.offset(150.dp, 150.dp), exit = exit) {
                        SolidElement(Modifier)
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            visible = false
            mainClock.advanceTimeBy(300)
            exit = KapowExit.shatter()
            mainClock.advanceTimeBy(3000)
            assertEquals(Color.White, capture().colorAt(200, 200))
        }

    @Test
    fun reducedMotionFades() = runTransitionTest { checkReducedExitFades(KapowExit.disintegrate()) }

    @Test
    fun framesDoNotRecompose() =
        runTransitionTest { checkFramesDoNotRecompose(KapowEnter.None, KapowExit.disintegrate()) }

    @Test
    fun colorsAreReadPerColumnAndSkipTransparentPixels() {
        val image = ImageBitmap(100, 40)
        Canvas(image).drawRect(Rect(0f, 0f, 50f, 40f), Paint().apply { color = Color.Red })
        val colors = dustColors(image, 2, Color.Gray)
        assertEquals(listOf(listOf(Color.Red, Color.Red, Color.Red, Color.Red), listOf(Color.Gray)), colors)
    }

    @Test
    fun keyFrame() =
        runTransitionTest { keyFrame("disintegrate_exit_500", exit = KapowExit.disintegrate(), millis = 500) }
}
