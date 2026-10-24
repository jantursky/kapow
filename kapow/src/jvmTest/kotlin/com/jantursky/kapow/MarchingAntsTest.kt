package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MarchingAntsTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.marchingAnts(width = 4.dp)

    /** The colors along the element's left edge, inside the stroke. */
    private fun androidx.compose.ui.graphics.ImageBitmap.leftEdge(): List<Color> =
        (150 until 250).map { colorAt(151, it) }

    @Test
    fun dashesMoveAlongTheOutlineWhileOn() =
        runEffectTest {
            scene.show(this, effect)
            scene.on = true
            mainClock.advanceTimeBy(500)
            val first = capture().leftEdge()
            assertTrue(first.any { it.luminance > 0.9f }, "white dashes")
            assertTrue(first.any { it.luminance < 0.1f }, "black gaps")
            mainClock.advanceTimeBy(200)
            assertTrue(capture().leftEdge() != first, "the dashes moved")
        }

    @Test
    fun transparentGapsShowTheElement() =
        runEffectTest {
            scene.show(this, ConditionalEffect.marchingAnts(Color.Red, gapColor = Color.Transparent, width = 4.dp))
            scene.on = true
            mainClock.advanceTimeBy(500)
            assertTrue(capture().leftEdge().any { it == Cobalt })
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
    fun reducedMotionKeepsTheDashesStill() =
        runEffectTest {
            scene.show(this, effect, KapowMotion.Reduced)
            scene.on = true
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            val first = capture()
            assertTrue(first.leftEdge().any { it.luminance > 0.9f })
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
