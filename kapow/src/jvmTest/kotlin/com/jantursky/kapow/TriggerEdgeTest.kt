package com.jantursky.kapow

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TriggerEdgeTest {
    private fun androidx.compose.ui.test.ComposeUiTest.step() = mainClock.advanceTimeBy(100)

    @Test
    fun aRisingEdgePlaysOnTheChangesToTrue() =
        runEffectTest {
            val effect = RecordingEffect()
            var on by mutableStateOf(false)
            setEffectContent { Element(Modifier.edgeEffect(on, KapowEdge.Rising, effect)) }
            step()
            listOf(true, false, true, false).forEach {
                on = it
                step()
            }
            assertEquals(2, effect.log.size)
        }

    @Test
    fun aFallingEdgePlaysOnTheChangeToFalse() =
        runEffectTest {
            val effect = RecordingEffect()
            var on by mutableStateOf(true)
            setEffectContent { Element(Modifier.edgeEffect(on, KapowEdge.Falling, effect)) }
            step()
            assertEquals(0, effect.log.size)
            on = false
            step()
            assertEquals(1, effect.log.size)
            on = true
            step()
            assertEquals(1, effect.log.size)
        }

    @Test
    fun theFirstCompositionPlaysNothingByDefault() =
        runEffectTest {
            val effect = RecordingEffect()
            val trigger = KapowTrigger()
            setEffectContent {
                Element(Modifier.changeEffect(1, effect))
                Element(Modifier.changeEffect(trigger, effect))
                Element(Modifier.edgeEffect(true, KapowEdge.Rising, effect))
            }
            mainClock.advanceTimeBy(500)
            assertEquals(0, effect.log.size)
        }

    @Test
    fun playOnStartPlaysOnceOnTheFirstComposition() =
        runEffectTest {
            val value = RecordingEffect()
            val trigger = RecordingEffect()
            setEffectContent {
                Element(Modifier.changeEffect(1, value, playOnStart = true))
                Element(Modifier.changeEffect(KapowTrigger(), trigger, playOnStart = true))
            }
            mainClock.advanceTimeBy(500)
            assertEquals(1, value.log.size)
            assertEquals(1, trigger.log.size)
        }

    @Test
    fun anEdgeWithPlayOnStartPlaysOnlyWhenTheConditionMatches() =
        runEffectTest {
            val matching = RecordingEffect()
            val other = RecordingEffect()
            setEffectContent {
                Element(Modifier.edgeEffect(true, KapowEdge.Rising, matching, playOnStart = true))
                Element(Modifier.edgeEffect(true, KapowEdge.Falling, other, playOnStart = true))
            }
            mainClock.advanceTimeBy(500)
            assertEquals(1, matching.log.size)
            assertEquals(0, other.log.size)
        }

    @Test
    fun aStartPlaysOncePerRowAndLaterPlaysAreRealChanges() =
        runEffectTest {
            val effect = RecordingEffect()
            val list = LazyListState()
            var count by mutableIntStateOf(0)
            setEffectContent {
                LazyColumn(state = list) {
                    items(200) { Element(Modifier.changeEffect(count, effect, playOnStart = true)) }
                }
            }
            mainClock.advanceTimeBy(100)
            // Four 100-dp items fit in the 400-dp window, and each plays its start.
            assertEquals(4, effect.log.size)
            mainClock.advanceTimeBy(500)
            assertEquals(4, effect.log.size, "no replay without a change")
            count++
            mainClock.advanceTimeBy(100)
            assertEquals(8, effect.log.size, "the visible rows play the change")
        }
}
