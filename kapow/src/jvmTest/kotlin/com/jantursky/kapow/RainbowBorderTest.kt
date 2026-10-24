package com.jantursky.kapow

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RainbowBorderTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.rainbowBorder(listOf(Color.Red, Color.Green), width = 6.dp)

    @Test
    fun theGradientTurnsAroundTheOutlineWhileOn() =
        runEffectTest {
            scene.show(this, effect)
            scene.on = true
            mainClock.advanceTimeBy(500)
            val first = capture()
            val left = first.colorAt(152, 200)
            val right = first.colorAt(247, 200)
            assertTrue(left != Cobalt && right != Cobalt, "stroked all around")
            assertTrue(left != right, "a different color on each side")
            mainClock.advanceTimeBy(700)
            assertTrue(capture().colorAt(247, 200) != right, "the colors moved on")
        }

    @Test
    fun itFollowsTheShape() =
        runEffectTest {
            scene.show(this, ConditionalEffect.rainbowBorder(width = 6.dp, shape = CircleShape))
            scene.on = true
            mainClock.advanceTimeBy(500)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(152, 152), "the corner stays clear")
            assertTrue(image.colorAt(152, 200) != Cobalt)
        }

    @Test
    fun itWindsDownToTheUntouchedElement() =
        runEffectTest {
            scene.show(this, effect)
            val before = capture()
            scene.on = true
            mainClock.advanceTimeBy(800)
            scene.on = false
            mainClock.advanceTimeBy(600)
            assertEquals(0, capture().pixelsDifferentFrom(before))
        }

    @Test
    fun reducedMotionKeepsTheGradientStill() =
        runEffectTest {
            scene.show(this, effect, KapowMotion.Reduced)
            scene.on = true
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            val first = capture()
            assertTrue(first.colorAt(152, 200) != Cobalt)
            mainClock.advanceTimeBy(700)
            assertEquals(0, capture().pixelsDifferentFrom(first))
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene.show(this, effect)
            assertTrue(scene.compositionsWhileOn(this) <= 1)
        }
}
