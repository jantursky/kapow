package com.jantursky.kapow

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
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** A flashing change effect that records the time of each play, from [now]. */
data class FlashProbe(
    val plays: MutableList<Long>,
    val now: () -> Long,
) : ChangeEffect() {
    override val cooldown: Duration get() = Duration.ZERO
    override val isFlashing: Boolean get() = true

    override fun createRunner(): ChangeRunner =
        object : ChangeRunner() {
            override fun fire(context: EffectContext) {
                plays += now()
            }
        }
}

@OptIn(ExperimentalTestApi::class)
class FlashLimitTest {
    private var value by mutableIntStateOf(0)
    private var limit by mutableStateOf<Int?>(null)

    private fun ComposeUiTest.scene(effect: ChangeEffect) {
        setEffectContent {
            CompositionLocalProvider(LocalKapowMaxFlashesPerSecond provides limit) {
                Element(Modifier.changeEffect(value, effect))
            }
        }
    }

    /** The plays are at least a limit's interval apart: 1/3 s, within the millisecond the clock rounds to. */
    private fun assertSpaced(plays: List<Long>) {
        plays.zipWithNext().forEach { (a, b) -> assertTrue(b - a >= 333, "plays at $a and $b ms are too close") }
    }

    /** Changes the value every [every] ms for [total] ms. */
    private fun ComposeUiTest.changeEvery(
        every: Long,
        total: Long,
    ) {
        var elapsed = 0L
        while (elapsed < total) {
            value++
            mainClock.advanceTimeBy(every)
            elapsed += every
        }
    }

    @Test
    fun offByDefault() =
        runEffectTest {
            val plays = mutableListOf<Long>()
            scene(FlashProbe(plays) { mainClock.currentTime })
            changeEvery(200, 1000)
            assertEquals(5, plays.size)
        }

    @Test
    fun rapidTriggersPlayAtMostTheLimit() =
        runEffectTest {
            val plays = mutableListOf<Long>()
            limit = 3
            scene(FlashProbe(plays) { mainClock.currentTime })
            changeEvery(100, 1000)
            assertTrue(plays.size >= 2, "played ${plays.size} times")
            assertSpaced(plays)
        }

    @Test
    fun slowTriggersAllPlay() =
        runEffectTest {
            val plays = mutableListOf<Long>()
            limit = 3
            scene(FlashProbe(plays) { mainClock.currentTime })
            changeEvery(1000, 3000)
            assertEquals(3, plays.size)
        }

    @Test
    fun anIgnoredTriggerIsNotQueued() =
        runEffectTest {
            val plays = mutableListOf<Long>()
            limit = 3
            scene(FlashProbe(plays) { mainClock.currentTime })
            value++
            mainClock.advanceTimeBy(50)
            value++
            mainClock.advanceTimeBy(2000)
            assertEquals(1, plays.size)
        }

    @Test
    fun aNonFlashingEffectIsNotLimited() =
        runEffectTest {
            val effect = RecordingEffect()
            limit = 1
            scene(effect)
            changeEvery(100, 1000)
            assertEquals(10, effect.log.size)
        }

    @Test
    fun aNestedProviderLimitsOnlyItsSubtree() =
        runEffectTest {
            val inside = mutableListOf<Long>()
            val outside = mutableListOf<Long>()
            setEffectContent {
                Box(Modifier.fillMaxSize()) {
                    Element(Modifier.changeEffect(value, FlashProbe(outside) { mainClock.currentTime }))
                    CompositionLocalProvider(LocalKapowMaxFlashesPerSecond provides KapowWcagMaxFlashesPerSecond) {
                        Element(
                            Modifier
                                .offset(
                                    150.dp,
                                    0.dp,
                                ).changeEffect(value, FlashProbe(inside) { mainClock.currentTime }),
                        )
                    }
                }
            }
            changeEvery(100, 1000)
            assertEquals(10, outside.size)
            assertTrue(inside.size >= 2, "inside played ${inside.size} times")
            assertSpaced(inside)
        }

    @Test
    fun aRepeatStartsItsPlaysAtLeastASpacingApart() =
        runEffectTest {
            val plays = mutableListOf<Long>()
            limit = 3
            val scene = LoopScene()
            setEffectContent {
                CompositionLocalProvider(LocalKapowMaxFlashesPerSecond provides limit) {
                    Box(Modifier.fillMaxSize().background(Color.White)) {
                        Element(
                            Modifier.conditionalEffect(
                                scene.on,
                                ConditionalEffect.repeat(
                                    FlashProbe(plays) {
                                        mainClock.currentTime
                                    },
                                    70.milliseconds,
                                ),
                            ),
                        )
                    }
                }
            }
            scene.on = true
            repeat(62) { mainClock.advanceTimeByFrame() }
            assertTrue(plays.size >= 3, "played ${plays.size} times in a second")
            assertSpaced(plays)
        }

    @Test
    fun aRepeatWithoutALimitKeepsItsOwnPace() =
        runEffectTest {
            val plays = mutableListOf<Long>()
            val scene = LoopScene()
            setEffectContent {
                Element(
                    Modifier.conditionalEffect(
                        scene.on,
                        ConditionalEffect.repeat(
                            FlashProbe(plays) {
                                mainClock.currentTime
                            },
                            100.milliseconds,
                        ),
                    ),
                )
            }
            scene.on = true
            repeat(62) { mainClock.advanceTimeByFrame() }
            assertTrue(plays.size >= 6, "played ${plays.size} times")
        }

    private fun ComposeUiTest.glitchLooks(): Int {
        var looks = 0
        var previous = capture()
        repeat(62) {
            mainClock.advanceTimeByFrame()
            val next = capture()
            if (next.pixelsDifferentFrom(previous) > 0) looks++
            previous = next
        }
        return looks
    }

    private fun ComposeUiTest.glitchScene() {
        setEffectContent {
            CompositionLocalProvider(LocalKapowMaxFlashesPerSecond provides limit) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    SolidElement(
                        Modifier.offset(150.dp, 150.dp).changeEffect(value, ChangeEffect.glitch(1.seconds, 8.dp)),
                    )
                }
            }
        }
    }

    @Test
    fun glitchChangesItsLookAtMostTheLimitPerSecond() =
        runEffectTest {
            limit = 3
            glitchScene()
            mainClock.advanceTimeByFrame()
            value++
            val looks = glitchLooks()
            assertTrue(looks in 1..5, "the look changed $looks times in a second")
        }

    @Test
    fun glitchKeepsItsPaceWithoutALimit() =
        runEffectTest {
            glitchScene()
            mainClock.advanceTimeByFrame()
            value++
            val looks = glitchLooks()
            assertTrue(looks >= 12, "the look changed $looks times in a second")
        }
}
