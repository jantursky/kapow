package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BreatheTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.breathe(scale = 1.2f, float = 10.dp)

    @Test
    fun theElementSwellsAndFloatsWhileOn() =
        runEffectTest {
            scene.show(this, effect)
            val rest = capture()
            scene.on = true
            mainClock.advanceTimeBy(1500)
            val full = capture()
            assertEquals(Color.White, rest.colorAt(145, 200))
            assertEquals(Cobalt, full.colorAt(145, 200), "wider than at rest")
            assertEquals(Cobalt, full.colorAt(200, 140), "floated above its top edge")
            mainClock.advanceTimeBy(1500)
            assertEquals(Color.White, capture().colorAt(145, 200), "back to its size after a breath")
        }

    @Test
    fun itSettlesBackToRestWithoutAJump() =
        runEffectTest {
            scene.show(this, effect)
            val rest = capture()
            scene.on = true
            mainClock.advanceTimeBy(1500)
            scene.on = false
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            val going = capture()
            assertEquals(Cobalt, going.colorAt(145, 200), "not snapped back")
            mainClock.advanceTimeBy(3000)
            assertEquals(0, capture().pixelsDifferentFrom(rest))
        }

    @Test
    fun reducedMotionKeepsTheElementStill() =
        runEffectTest {
            scene.show(this, effect, KapowMotion.Reduced)
            val rest = capture()
            scene.on = true
            mainClock.advanceTimeBy(1500)
            assertEquals(0, capture().pixelsDifferentFrom(rest))
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene.show(this, effect)
            assertTrue(scene.compositionsWhileOn(this) <= 1)
        }
}
