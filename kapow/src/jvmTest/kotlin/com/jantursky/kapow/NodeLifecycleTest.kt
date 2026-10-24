package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/** Nodes that are reused, moved or reconfigured while an effect is pending or playing. */
@OptIn(ExperimentalTestApi::class)
class NodeLifecycleTest {
    @Test
    fun scrollingALazyListPlaysNothing() =
        runEffectTest {
            val effect = RecordingEffect()
            val list = LazyListState()
            setEffectContent {
                LazyColumn(state = list) { items(200) { Element(Modifier.changeEffect(it, effect)) } }
            }
            repeat(10) {
                list.requestScrollToItem((it + 1) * 15)
                mainClock.advanceTimeBy(100)
            }
            assertEquals(0, effect.log.size)
        }

    @Test
    fun reusedItemsStillPlayARealChange() =
        runEffectTest {
            val effect = RecordingEffect()
            val list = LazyListState()
            var count by mutableIntStateOf(0)
            setEffectContent {
                LazyColumn(state = list) { items(200) { Element(Modifier.changeEffect(count, effect)) } }
            }
            repeat(5) {
                list.requestScrollToItem((it + 1) * 15)
                mainClock.advanceTimeBy(100)
            }
            count++
            mainClock.advanceTimeBy(100)
            // Four 100-dp items fit in the 400-dp window.
            assertEquals(4, effect.log.size)
        }

    @Test
    fun anEffectCutOffByAMoveDoesNotStayFrozen() =
        runEffectTest {
            var value by mutableIntStateOf(0)
            var moved by mutableStateOf(false)
            setEffectContent {
                val element =
                    remember {
                        movableContentOf {
                            SolidElement(
                                Modifier.offset(150.dp, 150.dp).changeEffect(value, ChangeEffect.spin(SpinAxis.Y)),
                            )
                        }
                    }
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    if (moved) Box { element() } else element()
                }
            }
            value++
            mainClock.advanceTimeBy(120)
            assertEquals(Color.White, capture().colorAt(153, 200), "turning")
            moved = true
            mainClock.advanceTimeBy(8000)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(153, 200))
            assertEquals(Cobalt, image.colorAt(200, 200))
        }

    @Test
    fun aDelayedPlayIsDroppedWhenTheEffectIsReplaced() =
        runEffectTest {
            val first = RecordingEffect()
            val second = RecordingEffect(cooldown = 1.milliseconds)
            var value by mutableIntStateOf(0)
            var effect: ChangeEffect by mutableStateOf(first.delay(300.milliseconds))
            setEffectContent { Element(Modifier.changeEffect(value, effect)) }
            value = 1
            mainClock.advanceTimeBy(100)
            effect = second
            mainClock.advanceTimeBy(500)
            assertEquals(0, first.log.size)
            assertEquals(0, second.log.size)
        }

    @Test
    fun aDelayedPlayIsDroppedWhenTheEffectIsTurnedOff() =
        runEffectTest {
            val effect = RecordingEffect()
            var value by mutableIntStateOf(0)
            var enabled by mutableStateOf(true)
            setEffectContent {
                Element(
                    Modifier.changeEffect(value, effect.delay(300.milliseconds), isEnabled = enabled),
                )
            }
            value = 1
            mainClock.advanceTimeBy(100)
            enabled = false
            mainClock.advanceTimeBy(500)
            assertTrue(effect.log.isEmpty())
        }

    @Test
    fun aConditionalEffectKeepsRunningOnAReusedItem() =
        runEffectTest {
            val list = LazyListState()
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    LazyColumn(state = list) {
                        items(200) {
                            SolidElement(Modifier.conditionalEffect(true, ConditionalEffect.glow(Color.Red)))
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            list.requestScrollToItem(30)
            mainClock.advanceTimeBy(500)
            // The halo spreads past the element's right edge.
            assertTrue(capture().colorAt(103, 50) != Color.White)
        }
}
