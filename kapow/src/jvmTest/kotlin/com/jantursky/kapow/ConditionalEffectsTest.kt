package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class ConditionalEffectsTest {
    private var on by mutableStateOf(false)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        effect: ConditionalEffect,
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            KapowParticleLayer(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    counter.Count()
                    SolidElement(Modifier.offset(150.dp, 150.dp).conditionalEffect(on, effect))
                }
            }
        }
    }

    /** Pixels above the element that aren't white, where only smoke can be. */
    private fun ImageBitmap.markedAbove(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 0 until 150) for (x in 0 until 400) if (pixels[x, y] != Color.White) n++
        return n
    }

    @Test
    fun repeatPlaysAtOnceThenEveryInterval() =
        runEffectTest {
            val effect = RecordingEffect()
            scene(ConditionalEffect.repeat(effect, every = 2.seconds))
            on = true
            mainClock.advanceTimeBy(5000)
            assertEquals(3, effect.log.size, "plays at 0, 2 and 4 s")
            on = false
            mainClock.advanceTimeBy(5000)
            assertEquals(3, effect.log.size)
        }

    @Test
    fun repeatKeepsTheWrappedCooldown() =
        runEffectTest {
            val effect = RecordingEffect(cooldown = 1.seconds)
            scene(ConditionalEffect.repeat(effect, every = 100.milliseconds))
            on = true
            mainClock.advanceTimeBy(1500)
            assertEquals(2, effect.log.size)
        }

    @Test
    fun repeatHonorsTheWrappedDelay() =
        runEffectTest {
            val effect = RecordingEffect()
            scene(ConditionalEffect.repeat(effect.delay(500.milliseconds), every = 2.seconds))
            on = true
            mainClock.advanceTimeBy(300)
            assertEquals(0, effect.log.size)
            mainClock.advanceTimeBy(400)
            assertEquals(1, effect.log.size)
        }

    @Test
    fun repeatPlaysTheReducedVersion() =
        runEffectTest {
            val effect = RecordingEffect()
            scene(ConditionalEffect.repeat(effect, every = 1.seconds), KapowMotion.Reduced)
            on = true
            mainClock.advanceTimeBy(100)
            assertEquals(listOf(true), effect.log)
        }

    @Test
    fun pushDownShrinksAndSpringsBack() =
        runEffectTest {
            scene(ConditionalEffect.pushDown())
            on = true
            mainClock.advanceTimeBy(600)
            assertEquals(Color.White, capture().colorAt(151, 200), "edge pulled in")
            assertEquals(Cobalt, capture().colorAt(200, 200))
            on = false
            mainClock.advanceTimeBy(1000)
            assertEquals(Cobalt, capture().colorAt(151, 200))
        }

    @Test
    fun reducedPushDownDimsInPlace() =
        runEffectTest {
            scene(ConditionalEffect.pushDown(), KapowMotion.Reduced)
            on = true
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            val image = capture()
            assertTrue(image.colorAt(151, 200) != Color.White, "not shrunk")
            assertTrue(image.colorAt(200, 200).luminance > Cobalt.luminance + 0.05f, "dimmed over white")
        }

    @Test
    fun smokeRisesWhileOnAndClearsAfter() =
        runEffectTest {
            scene(ConditionalEffect.smoke(Color.Black))
            on = true
            mainClock.advanceTimeBy(800)
            assertTrue(capture().markedAbove() > 0, "smoke above the element")
            on = false
            mainClock.advanceTimeBy(2500)
            assertEquals(0, capture().markedAbove())
        }

    @Test
    fun reducedSmokeEmitsNothing() =
        runEffectTest {
            scene(ConditionalEffect.smoke(Color.Black), KapowMotion.Reduced)
            on = true
            mainClock.advanceTimeBy(800)
            assertEquals(0, capture().markedAbove())
        }

    @Test
    fun swappingTheEffectWhileOnStartsTheNewOne() =
        runEffectTest {
            var effect by mutableStateOf(ConditionalEffect.glow(Color.Red))
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    SolidElement(Modifier.offset(150.dp, 150.dp).conditionalEffect(on, effect))
                }
            }
            on = true
            mainClock.advanceTimeBy(600)
            effect = ConditionalEffect.pushDown()
            mainClock.advanceTimeBy(600)
            val image = capture()
            assertEquals(Color.White, image.colorAt(151, 200), "pressed")
            assertEquals(Color.White, image.colorAt(258, 200), "the halo is gone")
        }

    @Test
    fun anEffectThatStartsOnIsOnRightAway() =
        runEffectTest {
            on = true
            scene(ConditionalEffect.pushDown())
            mainClock.advanceTimeBy(600)
            assertEquals(Color.White, capture().colorAt(151, 200))
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene(ConditionalEffect.smoke())
            mainClock.advanceTimeByFrame()
            val before = counter.count
            on = true
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }
}
