package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GrayscaleTest {
    private val scene = LoopScene()

    private val Color.isGray: Boolean get() = abs(red - green) < 0.02f && abs(green - blue) < 0.02f

    @Test
    fun theElementTurnsGrayWhileOnAndBackAfter() =
        runEffectTest {
            scene.show(this, ConditionalEffect.grayscale())
            scene.on = true
            mainClock.advanceTimeBy(100)
            val halfway = capture().colorAt(200, 200)
            assertTrue(halfway != Cobalt && !halfway.isGray, "fading: $halfway")
            mainClock.advanceTimeBy(400)
            assertTrue(capture().colorAt(200, 200).isGray)
            scene.on = false
            mainClock.advanceTimeBy(500)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun aPartialAmountKeepsSomeColor() =
        runEffectTest {
            scene.show(this, ConditionalEffect.grayscale(amount = 0.5f))
            scene.on = true
            mainClock.advanceTimeBy(500)
            val color = capture().colorAt(200, 200)
            assertTrue(color.blue > color.red + 0.1f && color.blue < Cobalt.blue, "less blue: $color")
        }

    @Test
    fun reducedMotionTurnsGrayWithoutAFade() =
        runEffectTest {
            scene.show(this, ConditionalEffect.grayscale(), KapowMotion.Reduced)
            scene.on = true
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            assertTrue(capture().colorAt(200, 200).isGray)
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene.show(this, ConditionalEffect.grayscale())
            assertTrue(scene.compositionsWhileOn(this) <= 1)
        }
}
