package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TiltTest {
    private var clicks = 0
    private var enabled by mutableStateOf(true)
    private val counter = CompositionCounter()

    // The card fills 100..300 in both directions.
    private fun ComposeUiTest.scene(
        motion: KapowMotion = KapowMotion.Full,
        sheen: Color? = null,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                Box(
                    Modifier
                        .offset(100.dp, 100.dp)
                        .size(200.dp)
                        .testTag("card")
                        .kapowTilt(maxAngle = 25f, sheen = sheen, enabled = enabled)
                        .background(Cobalt)
                        .clickable(remember { MutableInteractionSource() }, indication = null) { clicks++ },
                )
            }
        }
    }

    /** How many rows of the column [x] hold the card. */
    private fun ImageBitmap.heightAt(x: Int): Int = (0 until height).count { y -> colorAt(x, y).luminance < 0.9f }

    private fun ImageBitmap.matches(other: ImageBitmap): Boolean {
        for (x in 0 until width step 2) {
            for (y in 0 until height step 2) {
                if (colorAt(x, y) !=
                    other.colorAt(x, y)
                ) {
                    return false
                }
            }
        }
        return true
    }

    private fun ComposeUiTest.hover(
        x: Float,
        y: Float,
    ) {
        onNodeWithTag("card").performMouseInput { moveTo(Offset(x, y)) }
        mainClock.advanceTimeBy(1200)
    }

    @Test
    fun thePointerSideDips() =
        runEffectTest {
            scene()
            mainClock.advanceTimeByFrame()
            val flat = capture()
            assertEquals(flat.heightAt(110), flat.heightAt(290), "flat to begin with")
            hover(195f, 5f)
            val tilted = capture()
            assertTrue(tilted.heightAt(290) < tilted.heightAt(110), "the right side the pointer is on turns away")
            assertTrue(tilted.colorAt(200, 200).luminance < 0.9f, "the card is still there")
        }

    @Test
    fun theElementSpringsBackWhenThePointerLeaves() =
        runEffectTest {
            scene()
            mainClock.advanceTimeByFrame()
            val flat = capture()
            hover(195f, 5f)
            assertTrue(!capture().matches(flat))
            onNodeWithTag("card").performMouseInput { exit(Offset(-50f, -50f)) }
            mainClock.advanceTimeBy(3000)
            assertTrue(capture().matches(flat), "flat again")
        }

    @Test
    fun aMiddlePointerDoesNotTilt() =
        runEffectTest {
            scene()
            mainClock.advanceTimeByFrame()
            val flat = capture()
            hover(100f, 100f)
            assertTrue(capture().matches(flat))
        }

    @Test
    fun clickableContentStillReceivesClicksAlsoWhileTilted() =
        runEffectTest {
            scene()
            onNodeWithTag("card").performClick()
            assertEquals(1, clicks)
            hover(180f, 20f)
            onNodeWithTag("card").performClick()
            assertEquals(2, clicks)
        }

    @Test
    fun theSheenLightsTheSideAwayFromThePointer() =
        runEffectTest {
            scene(sheen = Color.White)
            mainClock.advanceTimeByFrame()
            hover(195f, 5f)
            val image = capture()
            // The pointer is at the top right; the light is at the bottom left.
            assertTrue(image.colorAt(150, 250).luminance > image.colorAt(250, 150).luminance + 0.03f)
        }

    @Test
    fun noSheenIsDrawnWhenItIsOff() =
        runEffectTest {
            scene(sheen = null)
            mainClock.advanceTimeByFrame()
            hover(195f, 5f)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(200, 200))
        }

    @Test
    fun disabledStaysFlat() =
        runEffectTest {
            enabled = false
            scene(sheen = Color.White)
            mainClock.advanceTimeByFrame()
            val flat = capture()
            hover(195f, 5f)
            assertTrue(capture().matches(flat))
        }

    @Test
    fun disablingWhileTiltedGoesFlatAtOnce() =
        runEffectTest {
            enabled = true
            scene()
            mainClock.advanceTimeByFrame()
            val flat = capture()
            hover(195f, 5f)
            enabled = false
            mainClock.advanceTimeBy(50)
            assertTrue(capture().matches(flat))
        }

    @Test
    fun reducedMotionStaysFlat() =
        runEffectTest {
            scene(KapowMotion.Reduced, sheen = Color.White)
            mainClock.advanceTimeByFrame()
            val flat = capture()
            hover(195f, 5f)
            assertTrue(capture().matches(flat))
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene(sheen = Color.White)
            mainClock.advanceTimeByFrame()
            val before = counter.count
            hover(195f, 5f)
            hover(20f, 150f)
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun parametersAreChecked() {
        assertFailsWith<IllegalArgumentException> { Modifier.kapowTilt(maxAngle = -1f) }
        assertFailsWith<IllegalArgumentException> { Modifier.kapowTilt(perspective = 0.dp) }
        assertFailsWith<IllegalArgumentException> { Modifier.kapowTilt(damping = 0f) }
    }
}
