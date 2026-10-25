package com.jantursky.kapow

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class TuningTest {
    private var value by mutableIntStateOf(0)
    private var on by mutableStateOf(false)
    private var progress by mutableFloatStateOf(0f)

    @Test
    fun timesPlaysTheSeriesWithAGap() =
        runEffectTest {
            val effect = RecordingEffect()
            setEffectContent { Element(Modifier.changeEffect(value, effect.times(3, 200.milliseconds))) }
            value = 1
            mainClock.advanceTimeBy(100)
            assertEquals(1, effect.log.size, "first play at once")
            mainClock.advanceTimeBy(200)
            assertEquals(2, effect.log.size)
            mainClock.advanceTimeBy(200)
            assertEquals(3, effect.log.size)
            mainClock.advanceTimeBy(1000)
            assertEquals(3, effect.log.size)
        }

    @Test
    fun changesDuringTheSeriesAreIgnored() =
        runEffectTest {
            val effect = RecordingEffect()
            setEffectContent { Element(Modifier.changeEffect(value, effect.times(3, 200.milliseconds))) }
            value = 1
            mainClock.advanceTimeBy(100)
            value = 2
            mainClock.advanceTimeBy(150)
            assertEquals(2, effect.log.size, "no new series, only the first one's second play")
            mainClock.advanceTimeBy(1000)
            assertEquals(3, effect.log.size)
            value = 3
            mainClock.advanceTimeBy(100)
            assertEquals(4, effect.log.size, "a change after the series starts a new one")
        }

    @Test
    fun cooldownOverridesTheEffectsOwn() =
        runEffectTest {
            val effect = RecordingEffect()
            setEffectContent { Element(Modifier.changeEffect(value, effect.cooldown(500.milliseconds))) }
            value = 1
            mainClock.advanceTimeBy(100)
            value = 2
            mainClock.advanceTimeBy(100)
            assertEquals(1, effect.log.size)
            mainClock.advanceTimeBy(400)
            value = 3
            mainClock.advanceTimeBy(100)
            assertEquals(2, effect.log.size)
        }

    @Test
    fun wrappersCombineInAnyOrderWithDelay() {
        val a =
            RecordingEffect()
                .delay(100.milliseconds)
                .times(2, 50.milliseconds)
                .speed(2f)
                .cooldown(1.seconds)
        val b =
            RecordingEffect()
                .cooldown(1.seconds)
                .speed(2f)
                .times(2, 50.milliseconds)
                .delay(100.milliseconds)
        for (plan in listOf(a.plan(), b.plan())) {
            assertEquals(100.milliseconds, plan.delay)
            assertEquals(2, plan.times)
            assertEquals(50.milliseconds, plan.gap)
            assertEquals(2f, plan.speed)
            assertEquals(1.seconds, plan.cooldown)
        }
    }

    @Test
    fun speedsMultiplyAndOutermostWins() {
        val plan =
            RecordingEffect()
                .speed(2f)
                .speed(3f)
                .reducedMotion(ReducedMotionMode.Fade)
                .plan()
        assertEquals(6f, plan.speed)
        assertEquals(ReducedMotionMode.Fade, plan.mode)
    }

    @Test
    fun theSeriesBlocksChangesForItsWholeLength() {
        val plan = RecordingEffect().times(3, 200.milliseconds).plan()
        assertEquals(400.milliseconds, plan.gateWindow)
    }

    @Test
    fun speedAndModeReachTheRunner() =
        runEffectTest {
            val seen = mutableListOf<Pair<Float, ReducedMotionMode>>()
            val effect = ContextProbe(seen)
            setEffectContent {
                Element(Modifier.changeEffect(value, effect.speed(2f).reducedMotion(ReducedMotionMode.EndState)))
            }
            value = 1
            mainClock.advanceTimeBy(100)
            assertEquals(listOf(2f to ReducedMotionMode.EndState), seen)
        }

    @Test
    fun nothingModeSkipsThePlayUnderReducedMotion() =
        runEffectTest {
            val effect = RecordingEffect()
            setEffectContent(KapowMotion.Reduced) {
                Element(Modifier.changeEffect(value, effect.reducedMotion(ReducedMotionMode.Nothing)))
            }
            value = 1
            mainClock.advanceTimeBy(100)
            assertEquals(0, effect.log.size)
        }

    @Test
    fun nothingModeStillPlaysWithFullMotion() =
        runEffectTest {
            val effect = RecordingEffect()
            setEffectContent(KapowMotion.Full) {
                Element(Modifier.changeEffect(value, effect.reducedMotion(ReducedMotionMode.Nothing)))
            }
            value = 1
            mainClock.advanceTimeBy(100)
            assertEquals(1, effect.log.size)
        }

    @Test
    fun feedbackStillFiresWhenNothingSkipsThePlay() =
        runEffectTest {
            val events = mutableListOf<KapowFeedbackEvent>()
            val effect = RecordingEffect().reducedMotion(ReducedMotionMode.Nothing)
            setEffectContent(KapowMotion.Reduced) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalKapowFeedback provides KapowFeedback { events += it },
                ) {
                    Element(Modifier.changeEffect(value, effect, feedback = KapowFeedbackEvent.Success))
                }
            }
            value = 1
            mainClock.advanceTimeBy(100)
            assertEquals(1, events.size)
        }

    @Test
    fun conditionalStartDelayWaitsBeforeTheEffectStarts() =
        runEffectTest {
            val log = mutableListOf<Boolean>()
            setEffectContent { Element(Modifier.conditionalEffect(on, ActiveProbe(log).startDelay(300.milliseconds))) }
            on = true
            mainClock.advanceTimeBy(200)
            assertTrue(log.isEmpty())
            mainClock.advanceTimeBy(200)
            assertEquals(listOf(true), log)
            on = false
            mainClock.advanceTimeBy(100)
            assertEquals(listOf(true, false), log)
        }

    @Test
    fun conditionalTurnedOffDuringTheDelayNeverStarts() =
        runEffectTest {
            val log = mutableListOf<Boolean>()
            setEffectContent { Element(Modifier.conditionalEffect(on, ActiveProbe(log).startDelay(300.milliseconds))) }
            on = true
            mainClock.advanceTimeBy(100)
            on = false
            mainClock.advanceTimeBy(500)
            assertTrue(true !in log, "never started: $log")
        }

    @Test
    fun conditionalFadeAndSpeedReachTheRunner() =
        runEffectTest {
            val seen = mutableListOf<Triple<Float, Any?, Any?>>()
            val effect = ContextProbeConditional(seen).fadeIn(1.seconds).fadeOut(2.seconds).speed(3f)
            setEffectContent { Element(Modifier.conditionalEffect(on, effect)) }
            on = true
            mainClock.advanceTimeBy(100)
            assertEquals(Triple(3f, 1.seconds, 2.seconds), seen.last())
        }

    @Test
    fun conditionalNothingModeDoesNotStartUnderReducedMotion() =
        runEffectTest {
            val log = mutableListOf<Boolean>()
            setEffectContent(KapowMotion.Reduced) {
                Element(Modifier.conditionalEffect(on, ActiveProbe(log).reducedMotion(ReducedMotionMode.Nothing)))
            }
            on = true
            mainClock.advanceTimeBy(500)
            assertTrue(log.isEmpty())
        }

    @Test
    fun progressCurveReshapesTheProgress() =
        runEffectTest {
            val seen = mutableListOf<Float>()
            val effect = ProgressProbe(seen).curve(FastOutSlowInEasing)
            setEffectContent { Box(Modifier.fillMaxSize()) { Element(Modifier.progressEffect(effect) { progress }) } }
            progress = 0.5f
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            assertEquals(FastOutSlowInEasing.transform(0.5f), seen.last(), 0.0001f)
        }

    @Test
    fun smoothedProgressFollowsWithASpring() =
        runEffectTest {
            val seen = mutableListOf<Float>()
            val effect = ProgressProbe(seen).smoothed()
            setEffectContent { Box(Modifier.fillMaxSize()) { Element(Modifier.progressEffect(effect) { progress }) } }
            mainClock.advanceTimeBy(50)
            progress = 1f
            mainClock.advanceTimeBy(100)
            val mid = seen.last()
            assertTrue(mid > 0f && mid < 1f, "still on its way after 100 ms: $mid")
            mainClock.advanceTimeBy(3000)
            assertEquals(1f, seen.last(), 0.01f)
        }

    @Test
    fun invalidArgumentsAreRejected() {
        val effect = RecordingEffect()
        assertFailsWith<IllegalArgumentException> { effect.cooldown((-1).milliseconds) }
        assertFailsWith<IllegalArgumentException> { effect.times(0) }
        assertFailsWith<IllegalArgumentException> { effect.times(2, (-1).milliseconds) }
        assertFailsWith<IllegalArgumentException> { effect.speed(0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.glow().fadeIn((-1).milliseconds) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.iris().smoothed(dampingRatio = 0f) }
    }

    @Test
    fun wrappedEffectsAreEqualByValue() {
        assertEquals(
            ChangeEffect.flash().times(2, 100.milliseconds).speed(2f),
            ChangeEffect.flash().times(2, 100.milliseconds).speed(2f),
        )
        assertEquals(ConditionalEffect.glow().fadeIn(1.seconds), ConditionalEffect.glow().fadeIn(1.seconds))
    }

    @Test
    fun theOldFlashSignatureStillDrawsTheSameAsTheNewDefaults() {
        assertEquals(ChangeEffect.flash(Color.White, 400.milliseconds), ChangeEffect.flash(peak = 0.85f))
    }

    @Test
    fun flashPeakIsValidated() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.flash(peak = 0f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.flash(peak = 1.5f) }
    }
}
