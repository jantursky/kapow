package com.jantursky.kapow

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.SpotlightRunner
import com.jantursky.kapow.internal.ConditionalRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SpotlightTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.spotlight(Color.Black.copy(alpha = 0.6f), padding = 8.dp, softness = 24.dp)

    /** The spotlight drawn with stacked rings, as on Android before 12. */
    private val stacked =
        object : ConditionalEffect() {
            override fun createRunner(): ConditionalRunner =
                SpotlightRunner(Color.Black.copy(alpha = 0.6f), CircleShape, 8.dp, 24.dp) { false }
        }

    private fun ImageBitmap.assertSpotlit() {
        val far = colorAt(20, 20).luminance
        assertTrue(far in 0.3f..0.5f, "the window is dimmed far from the element: $far")
        assertEquals(Cobalt, colorAt(200, 200), "the element stays clear")
        assertEquals(Cobalt, colorAt(151, 200))
        assertTrue(colorAt(146, 200).luminance > 0.9f, "clear just past the element")
        val edge = colorAt(126, 200).luminance
        assertTrue(edge > far + 0.05f && edge < 0.95f, "a soft edge: $edge")
    }

    @Test
    fun everythingAroundTheElementDimsWhileOn() =
        runEffectTest {
            scene.show(this, effect)
            scene.on = true
            mainClock.advanceTimeBy(500)
            capture().assertSpotlit()
        }

    @Test
    fun stackedRingsDimTheSameWay() =
        runEffectTest {
            scene.show(this, stacked)
            scene.on = true
            mainClock.advanceTimeBy(500)
            val image = capture()
            assertTrue(image.colorAt(20, 20).luminance in 0.3f..0.5f)
            assertTrue(image.colorAt(146, 200).luminance > 0.9f)
            assertTrue(image.colorAt(152, 152).luminance < 0.95f, "the circle leaves the corner dimmed")
        }

    @Test
    fun itWindsDownToTheUntouchedWindow() =
        runEffectTest {
            scene.show(this, effect)
            val before = capture()
            scene.on = true
            mainClock.advanceTimeBy(150)
            val halfway = capture().colorAt(20, 20).luminance
            assertTrue(halfway > 0.5f && halfway < 1f, "fading in: $halfway")
            mainClock.advanceTimeBy(500)
            scene.on = false
            mainClock.advanceTimeBy(500)
            assertEquals(0, capture().pixelsDifferentFrom(before))
        }

    @Test
    fun reducedMotionDimsWithoutAFade() =
        runEffectTest {
            scene.show(this, effect, KapowMotion.Reduced)
            scene.on = true
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            capture().assertSpotlit()
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene.show(this, effect)
            assertTrue(scene.compositionsWhileOn(this) <= 1)
        }
}
