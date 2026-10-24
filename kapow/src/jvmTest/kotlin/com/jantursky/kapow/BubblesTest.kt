package com.jantursky.kapow

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BubblesTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.bubbles()

    @Test
    fun bubblesRiseWhileOnAndClearAfter() =
        runEffectTest {
            scene.show(this, effect)
            scene.on = true
            mainClock.advanceTimeBy(1000)
            assertTrue(capture().nonWhiteAbove() > 0, "bubbles above the element")
            scene.on = false
            mainClock.advanceTimeBy(2000)
            assertEquals(0, capture().nonWhiteAbove())
        }

    @Test
    fun reducedMotionShowsNone() =
        runEffectTest {
            scene.show(this, effect, KapowMotion.Reduced)
            scene.on = true
            mainClock.advanceTimeBy(1000)
            assertEquals(0, capture().nonWhiteAbove())
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene.show(this, effect)
            assertTrue(scene.compositionsWhileOn(this) <= 1)
        }
}
