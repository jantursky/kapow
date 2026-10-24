package com.jantursky.kapow

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class ChangeEffectTest {
    @Test
    fun firstCompositionIsSilent() =
        runEffectTest {
            val effect = RecordingEffect()
            setEffectContent { Element(Modifier.changeEffect(3, effect)) }
            mainClock.advanceTimeBy(100)
            assertEquals(0, effect.log.size)
        }

    @Test
    fun aChangeFiresOnce() =
        runEffectTest {
            val effect = RecordingEffect()
            var value by mutableIntStateOf(3)
            setEffectContent { Element(Modifier.changeEffect(value, effect)) }
            value = 4
            mainClock.advanceTimeBy(100)
            assertEquals(1, effect.log.size)
        }

    @Test
    fun anEqualValueDoesNothing() =
        runEffectTest {
            val effect = RecordingEffect()
            var value by mutableStateOf(listOf(1, 2))
            setEffectContent { Element(Modifier.changeEffect(value, effect)) }
            value = listOf(1, 2)
            mainClock.advanceTimeBy(100)
            assertEquals(0, effect.log.size)
        }

    @Test
    fun disabledChangesAreRecordedButDoNotPlay() =
        runEffectTest {
            val effect = RecordingEffect()
            var value by mutableIntStateOf(0)
            var enabled by mutableStateOf(false)
            setEffectContent { Element(Modifier.changeEffect(value, effect, isEnabled = enabled)) }
            value = 1
            mainClock.advanceTimeBy(100)
            enabled = true
            mainClock.advanceTimeBy(100)
            assertEquals(0, effect.log.size)
            value = 2
            mainClock.advanceTimeBy(100)
            assertEquals(1, effect.log.size)
        }

    @Test
    fun changesInsideTheCooldownAreIgnored() =
        runEffectTest {
            val effect = RecordingEffect(cooldown = 500.milliseconds)
            var value by mutableIntStateOf(0)
            setEffectContent { Element(Modifier.changeEffect(value, effect)) }
            value = 1
            mainClock.advanceTimeBy(200)
            value = 2
            mainClock.advanceTimeBy(200)
            assertEquals(1, effect.log.size)
            mainClock.advanceTimeBy(200)
            value = 3
            mainClock.advanceTimeBy(100)
            assertEquals(2, effect.log.size)
        }

    @Test
    fun delayedEffectsWait() =
        runEffectTest {
            val effect = RecordingEffect()
            var value by mutableIntStateOf(0)
            setEffectContent { Element(Modifier.changeEffect(value, effect.delay(300.milliseconds))) }
            value = 1
            mainClock.advanceTimeBy(200)
            assertEquals(0, effect.log.size)
            mainClock.advanceTimeBy(200)
            assertEquals(1, effect.log.size)
        }

    @Test
    fun nestedDelaysAddUp() {
        val (inner, delay) = RecordingEffect().delay(100.milliseconds).delay(50.milliseconds).withoutDelay()
        assertEquals(RecordingEffect(), inner)
        assertEquals(150.milliseconds, delay)
    }

    @Test
    fun reducedMotionReachesTheEffect() =
        runEffectTest {
            val effect = RecordingEffect()
            var value by mutableIntStateOf(0)
            setEffectContent(KapowMotion.Reduced) { Element(Modifier.changeEffect(value, effect)) }
            value = 1
            mainClock.advanceTimeBy(100)
            assertEquals(listOf(true), effect.log)
        }

    @Test
    fun feedbackPlaysOncePerChangeAlsoWithReducedMotion() =
        runEffectTest {
            val events = mutableListOf<KapowFeedbackEvent>()
            var value by mutableIntStateOf(0)
            setEffectContent(KapowMotion.Reduced) {
                CompositionLocalProvider(LocalKapowFeedback provides KapowFeedback { events += it }) {
                    Element(Modifier.changeEffect(value, RecordingEffect(), feedback = KapowFeedbackEvent.ImpactLight))
                }
            }
            value = 1
            mainClock.advanceTimeBy(100)
            assertEquals(listOf<KapowFeedbackEvent>(KapowFeedbackEvent.ImpactLight), events)
        }

    @Test
    fun feedbackWithoutAHandlerIsANoOp() =
        runEffectTest {
            val effect = RecordingEffect()
            var value by mutableIntStateOf(0)
            setEffectContent { Element(Modifier.changeEffect(value, effect, feedback = KapowFeedbackEvent.Success)) }
            value = 1
            mainClock.advanceTimeBy(100)
            assertEquals(1, effect.log.size)
        }

    @Test
    fun replacingTheEffectKeepsTheValue() =
        runEffectTest {
            val first = RecordingEffect()
            val second = RecordingEffect(cooldown = 1.milliseconds)
            var effect: ChangeEffect by mutableStateOf(first)
            setEffectContent { Element(Modifier.changeEffect(1, effect)) }
            effect = second
            mainClock.advanceTimeBy(100)
            assertEquals(0, first.log.size + second.log.size)
        }
}
