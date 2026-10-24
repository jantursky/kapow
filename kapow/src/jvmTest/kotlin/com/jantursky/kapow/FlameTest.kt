package com.jantursky.kapow

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FlameTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.flame()

    @Test
    fun flamesBurnAboveTheElementWhileOnAndClearAfter() =
        runEffectTest {
            scene.show(this, effect)
            scene.on = true
            mainClock.advanceTimeBy(800)
            val image = capture()
            assertTrue(image.nonWhiteAbove() > 0, "flames above the element")
            val pixels = image.toPixelMap()
            val warm = (100 until 150).flatMap { y -> (150 until 250).map { x -> pixels[x, y] } }
            assertTrue(warm.any { it.red > it.blue + 0.2f }, "warm colors")
            scene.on = false
            mainClock.advanceTimeBy(1200)
            assertEquals(0, capture().nonWhiteAbove())
        }

    @Test
    fun reducedMotionShowsNone() =
        runEffectTest {
            scene.show(this, effect, KapowMotion.Reduced)
            scene.on = true
            mainClock.advanceTimeBy(800)
            assertEquals(0, capture().nonWhiteAbove())
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene.show(this, effect)
            assertTrue(scene.compositionsWhileOn(this) <= 1)
        }
}
