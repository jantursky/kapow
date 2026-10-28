package com.jantursky.kapow

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class InteractionEffectTest {
    @Test
    fun aConditionalEffectRunsWhileAPressHolds() =
        runEffectTest {
            val log = mutableListOf<Boolean>()
            val source = MutableInteractionSource()
            setEffectContent {
                Element(Modifier.conditionalEffect(source, KapowInteraction.Pressed, ActiveProbe(log)))
            }
            mainClock.advanceTimeBy(50)
            assertTrue(log.isEmpty(), "idle on attach")
            val press = PressInteraction.Press(Offset.Zero)
            source.tryEmit(press)
            mainClock.advanceTimeBy(50)
            assertEquals(listOf(true), log)
            source.tryEmit(PressInteraction.Release(press))
            mainClock.advanceTimeBy(50)
            assertEquals(listOf(true, false), log)
            val second = PressInteraction.Press(Offset.Zero)
            source.tryEmit(second)
            source.tryEmit(PressInteraction.Cancel(second))
            mainClock.advanceTimeBy(50)
            assertEquals(listOf(true, false, true, false), log)
        }

    @Test
    fun aSwappedSourceWindsTheEffectDownAndDropsTheOldOne() =
        runEffectTest {
            val log = mutableListOf<Boolean>()
            val a = MutableInteractionSource()
            val b = MutableInteractionSource()
            var source by mutableStateOf(a)
            setEffectContent {
                Element(Modifier.conditionalEffect(source, KapowInteraction.Hovered, ActiveProbe(log)))
            }
            a.tryEmit(HoverInteraction.Enter())
            mainClock.advanceTimeBy(50)
            assertEquals(listOf(true), log)
            source = b
            mainClock.advanceTimeBy(50)
            assertEquals(listOf(true, false), log)
            a.tryEmit(HoverInteraction.Enter())
            mainClock.advanceTimeBy(50)
            assertEquals(listOf(true, false), log, "events of the old source are ignored")
            b.tryEmit(HoverInteraction.Enter())
            mainClock.advanceTimeBy(50)
            assertEquals(listOf(true, false, true), log)
        }

    @Test
    fun aChangeEffectPlaysOncePerPressNotWhileHeld() =
        runEffectTest {
            val effect = RecordingEffect()
            val source = MutableInteractionSource()
            setEffectContent {
                Element(Modifier.changeEffect(source, KapowInteraction.Pressed, effect))
            }
            val first = PressInteraction.Press(Offset.Zero)
            source.tryEmit(first)
            mainClock.advanceTimeBy(5000)
            assertEquals(1, effect.log.size, "held")
            source.tryEmit(PressInteraction.Release(first))
            mainClock.advanceTimeBy(100)
            assertEquals(1, effect.log.size, "release plays nothing")
            source.tryEmit(PressInteraction.Press(Offset.Zero))
            mainClock.advanceTimeBy(100)
            assertEquals(2, effect.log.size)
        }

    @Test
    fun interactionsDoNotRecompose() =
        runEffectTest {
            val counter = CompositionCounter()
            val source = MutableInteractionSource()
            setEffectContent {
                counter.Count()
                Element(
                    Modifier
                        .changeEffect(source, KapowInteraction.Pressed, RecordingEffect())
                        .conditionalEffect(source, KapowInteraction.Hovered, ActiveProbe(mutableListOf())),
                )
            }
            mainClock.advanceTimeBy(50)
            val before = counter.count
            repeat(10) {
                val press = PressInteraction.Press(Offset.Zero)
                val enter = HoverInteraction.Enter()
                source.tryEmit(press)
                source.tryEmit(enter)
                mainClock.advanceTimeBy(50)
                source.tryEmit(PressInteraction.Release(press))
                source.tryEmit(HoverInteraction.Exit(enter))
                mainClock.advanceTimeBy(50)
            }
            assertEquals(before, counter.count)
        }
}
