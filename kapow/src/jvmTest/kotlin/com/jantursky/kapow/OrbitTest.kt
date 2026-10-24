package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.orbit(listOf(Color.Red))

    /** Pixels left or right of the element that aren't white, where only the orbit reaches. */
    private fun ImageBitmap.besides(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 100 until 300) for (x in 0 until 400) if ((x < 150 || x >= 250) && pixels[x, y] != Color.White) n++
        return n
    }

    @Test
    fun particlesCircleTheElementWhileOn() =
        runEffectTest {
            scene.show(this, effect)
            scene.on = true
            mainClock.advanceTimeBy(500)
            val first = capture()
            assertTrue(first.besides() > 0, "the orbit reaches past the element")
            mainClock.advanceTimeBy(300)
            assertTrue(capture().pixelsDifferentFrom(first) > 0, "the particles moved")
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
