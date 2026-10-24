package com.jantursky.kapow

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ConditionalSparklesTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.sparkles()

    @Test
    fun sparklesTwinkleWhileOnAndClearAfter() =
        runEffectTest {
            scene.show(this, effect)
            val before = capture()
            scene.on = true
            mainClock.advanceTimeBy(800)
            assertTrue(capture().pixelsDifferentFrom(before) > 0, "sparkles drawn")
            scene.on = false
            mainClock.advanceTimeBy(1500)
            assertEquals(0, capture().pixelsDifferentFrom(before))
        }

    @Test
    fun reducedMotionShowsNone() =
        runEffectTest {
            scene.show(this, effect, KapowMotion.Reduced)
            val before = capture()
            scene.on = true
            mainClock.advanceTimeBy(800)
            assertEquals(0, capture().pixelsDifferentFrom(before))
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene.show(this, effect)
            assertTrue(scene.compositionsWhileOn(this) <= 1)
        }
}
