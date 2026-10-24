package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BorderBeamTest {
    private val scene = LoopScene()
    private val effect = ConditionalEffect.borderBeam(Color.Red, width = 4.dp)

    @Test
    fun aBeamTravelsAroundTheOutlineWhileOn() =
        runEffectTest {
            scene.show(this, effect)
            val before = capture()
            scene.on = true
            mainClock.advanceTimeBy(500)
            val first = capture()
            assertTrue(first.pixelsDifferentFrom(before) > 0, "drawn")
            val track = first.colorAt(151, 200)
            assertTrue(track.red > Cobalt.red + 0.05f, "a faint red track all around: $track")
            mainClock.advanceTimeBy(500)
            assertTrue(capture().pixelsDifferentFrom(first) > 0, "the beam moved on")
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
    fun reducedMotionLightsTheWholeOutlineStill() =
        runEffectTest {
            scene.show(this, effect, KapowMotion.Reduced)
            scene.on = true
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            val first = capture()
            for ((x, y) in listOf(151 to 200, 248 to 200, 200 to 151, 200 to 248)) {
                assertTrue(first.colorAt(x, y).red > 0.5f, "lit at $x, $y")
            }
            mainClock.advanceTimeBy(700)
            assertEquals(0, capture().pixelsDifferentFrom(first), "nothing moves")
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene.show(this, effect)
            assertTrue(scene.compositionsWhileOn(this) <= 1)
        }

    @Test
    fun itKeepsRunningOnAReusedItem() =
        runEffectTest {
            val list = LazyListState()
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    LazyColumn(state = list) { items(200) { SolidElement(Modifier.conditionalEffect(true, effect)) } }
                }
            }
            mainClock.advanceTimeBy(500)
            list.requestScrollToItem(30)
            mainClock.advanceTimeBy(500)
            assertTrue(capture().colorAt(1, 50).red > Cobalt.red + 0.05f)
        }
}
