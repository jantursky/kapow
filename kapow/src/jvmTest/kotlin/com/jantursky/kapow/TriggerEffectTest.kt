package com.jantursky.kapow

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class TriggerEffectTest {
    @Test
    fun aFirePlaysTheEffectOnce() =
        runEffectTest {
            val effect = RecordingEffect()
            val trigger = KapowTrigger()
            setEffectContent { Element(Modifier.changeEffect(trigger, effect)) }
            mainClock.advanceTimeBy(50)
            assertEquals(0, effect.log.size, "nothing on the first composition")
            trigger.fire()
            mainClock.advanceTimeBy(100)
            assertEquals(1, effect.log.size)
        }

    @Test
    fun firesInSeparateFramesPlayEachTime() =
        runEffectTest {
            val effect = RecordingEffect()
            val trigger = KapowTrigger()
            setEffectContent { Element(Modifier.changeEffect(trigger, effect)) }
            repeat(3) {
                trigger.fire()
                mainClock.advanceTimeBy(100)
            }
            assertEquals(3, effect.log.size)
        }

    @Test
    fun aFireBeforeTheElementExistsPlaysNothingLater() =
        runEffectTest {
            val effect = RecordingEffect()
            val trigger = KapowTrigger()
            trigger.fire()
            setEffectContent { Element(Modifier.changeEffect(trigger, effect)) }
            mainClock.advanceTimeBy(500)
            assertEquals(0, effect.log.size)
        }

    @Test
    fun firingDoesNotRecompose() =
        runEffectTest {
            val counter = CompositionCounter()
            val inner = CompositionCounter()
            val trigger = KapowTrigger()
            setEffectContent {
                counter.Count()
                androidx.compose.foundation.layout.Box(
                    Modifier.changeEffect(trigger, RecordingEffect()),
                ) { inner.Count() }
            }
            mainClock.advanceTimeBy(50)
            val before = counter.count to inner.count
            repeat(10) {
                trigger.fire()
                mainClock.advanceTimeBy(100)
            }
            assertEquals(before, counter.count to inner.count)
        }

    @Test
    fun oneTriggerDrivesSeveralElementsAndStopsForDetachedOnes() =
        runEffectTest {
            val first = RecordingEffect()
            val second = RecordingEffect()
            val trigger = KapowTrigger()
            var showSecond by mutableStateOf(true)
            setEffectContent {
                Element(Modifier.changeEffect(trigger, first))
                if (showSecond) Element(Modifier.changeEffect(trigger, second))
            }
            trigger.fire()
            mainClock.advanceTimeBy(100)
            assertEquals(1, first.log.size)
            assertEquals(1, second.log.size)
            showSecond = false
            mainClock.advanceTimeBy(100)
            trigger.fire()
            mainClock.advanceTimeBy(100)
            assertEquals(2, first.log.size)
            assertEquals(1, second.log.size)
        }

    @Test
    fun theCooldownAppliesToFires() =
        runEffectTest {
            val effect = RecordingEffect(cooldown = 1.seconds)
            val trigger = KapowTrigger()
            setEffectContent { Element(Modifier.changeEffect(trigger, effect)) }
            trigger.fire()
            mainClock.advanceTimeBy(100)
            trigger.fire()
            mainClock.advanceTimeBy(100)
            assertEquals(1, effect.log.size)
            mainClock.advanceTimeBy(1000)
            trigger.fire()
            mainClock.advanceTimeBy(100)
            assertEquals(2, effect.log.size)
        }

    @Test
    fun theDelayAndTimesApplyToFires() =
        runEffectTest {
            val delayed = RecordingEffect()
            val repeated = RecordingEffect()
            val trigger = KapowTrigger()
            setEffectContent {
                Element(Modifier.changeEffect(trigger, delayed.delay(300.milliseconds)))
                Element(Modifier.changeEffect(trigger, repeated.times(3, 50.milliseconds)))
            }
            trigger.fire()
            mainClock.advanceTimeBy(100)
            assertEquals(0, delayed.log.size)
            mainClock.advanceTimeBy(400)
            assertEquals(1, delayed.log.size)
            assertEquals(3, repeated.log.size)
        }

    @Test
    fun aDisabledElementPlaysNothingAndSendsNoFeedback() =
        runEffectTest {
            val effect = RecordingEffect()
            val events = mutableListOf<KapowFeedbackEvent>()
            val trigger = KapowTrigger()
            setEffectContent {
                CompositionLocalProvider(LocalKapowFeedback provides KapowFeedback { events += it }) {
                    Element(
                        Modifier.changeEffect(
                            trigger,
                            effect,
                            isEnabled = false,
                            feedback = KapowFeedbackEvent.Success,
                        ),
                    )
                }
            }
            trigger.fire()
            mainClock.advanceTimeBy(200)
            assertTrue(effect.log.isEmpty())
            assertTrue(events.isEmpty())
        }

    @Test
    fun anEnabledElementSendsFeedbackForEachFire() =
        runEffectTest {
            val events = mutableListOf<KapowFeedbackEvent>()
            val trigger = KapowTrigger()
            setEffectContent {
                CompositionLocalProvider(LocalKapowFeedback provides KapowFeedback { events += it }) {
                    Element(Modifier.changeEffect(trigger, RecordingEffect(), feedback = KapowFeedbackEvent.Success))
                }
            }
            repeat(2) {
                trigger.fire()
                mainClock.advanceTimeBy(100)
            }
            assertEquals(2, events.size)
        }
}
