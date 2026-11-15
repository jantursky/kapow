package com.jantursky.kapow

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class, KapowTestApi::class)
class EffectsSwitchTest {
    private var value by mutableIntStateOf(0)
    private var visible by mutableStateOf(true)

    private fun ComposeUiTest.change(effectsOn: Boolean) {
        setEffectContent {
            CompositionLocalProvider(LocalKapowEffects provides effectsOn) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    SolidElement(Modifier.changeEffect(value, ChangeEffect.shine(angle = 0f, duration = 1.seconds)))
                }
            }
        }
    }

    @Test
    fun aChangeDrawsNothingWhenOff() =
        runEffectTest {
            change(effectsOn = false)
            val before = capture()
            value++
            mainClock.advanceTimeBy(500)
            val after = capture()
            for (x in 0 until 100 step 5) assertEquals(before.colorAt(x, 50), after.colorAt(x, 50))
        }

    @Test
    fun aChangeStillDrawsByDefault() =
        runEffectTest {
            change(effectsOn = true)
            val before = capture()
            value++
            mainClock.advanceTimeBy(500)
            val after = capture()
            assertNotEquals((0 until 100).map { before.colorAt(it, 50) }, (0 until 100).map { after.colorAt(it, 50) })
        }

    @Test
    fun aConditionDrawsNothingWhenOff() =
        runEffectTest {
            val log = mutableListOf<Boolean>()
            setEffectContent {
                CompositionLocalProvider(LocalKapowEffects provides false) {
                    SolidElement(Modifier.conditionalEffect(true, ActiveProbe(log)))
                }
            }
            val before = capture()
            mainClock.advanceTimeBy(500)
            assertEquals(before.colorAt(50, 50), capture().colorAt(50, 50))
        }

    @Test
    fun aProgressDrawsNothingWhenOff() =
        runEffectTest {
            val seen = mutableListOf<Float>()
            setEffectContent {
                CompositionLocalProvider(LocalKapowEffects provides false) {
                    SolidElement(Modifier.progressEffect(ProgressProbe(seen)) { 0.5f })
                }
            }
            mainClock.advanceTimeBy(100)
            assertEquals(emptyList(), seen)
        }

    @Test
    fun noFeedbackIsSentWhenOff() =
        runEffectTest {
            val events = mutableListOf<KapowFeedbackEvent>()
            setEffectContent {
                CompositionLocalProvider(
                    LocalKapowEffects provides false,
                    LocalKapowFeedback provides KapowFeedback { events += it },
                ) {
                    Element(
                        Modifier.changeEffect(value, ChangeEffect.shine(), feedback = KapowFeedbackEvent.ImpactLight),
                    )
                }
            }
            value++
            mainClock.advanceTimeBy(500)
            assertEquals(emptyList(), events)
        }

    @Test
    fun noFeedbackIsSentWhenOffForATransition() =
        runEffectTest {
            val events = mutableListOf<KapowFeedbackEvent>()
            visible = false
            setEffectContent {
                CompositionLocalProvider(
                    LocalKapowEffects provides false,
                    LocalKapowFeedback provides KapowFeedback { events += it },
                ) {
                    KapowAnimatedVisibility(visible, enter = KapowEnter.iris(), feedback = KapowFeedbackEvent.Success) {
                        SolidElement(Modifier)
                    }
                }
            }
            visible = true
            mainClock.advanceTimeBy(50)
            assertEquals(emptyList(), events)
        }

    private fun ComposeUiTest.scene(effectsOn: Boolean) {
        setEffectContent {
            CompositionLocalProvider(LocalKapowEffects provides effectsOn) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    KapowAnimatedVisibility(
                        visible,
                        Modifier.offset(150.dp, 150.dp),
                        KapowEnter.iris(),
                        KapowExit.poof(),
                        EnterTransition.None,
                        ExitTransition.None,
                    ) {
                        SolidElement(Modifier)
                    }
                }
            }
        }
    }

    @Test
    fun enteringContentIsVisibleAtOnceWhenOff() =
        runEffectTest {
            visible = false
            scene(effectsOn = false)
            visible = true
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            onNodeWithTag("element").assertIsDisplayed()
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun exitingContentLeavesAtOnceWhenOff() =
        runEffectTest {
            scene(effectsOn = false)
            visible = false
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            onNodeWithTag("element").assertDoesNotExist()
        }

    @Test
    fun exitingContentWaitsForItsEffectByDefault() =
        runEffectTest {
            scene(effectsOn = true)
            visible = false
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            onNodeWithTag("element").assertIsDisplayed()
        }
}
