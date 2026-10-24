package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class StrikeTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        motion: KapowMotion = KapowMotion.Full,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        setEffectContent(motion) {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    counter.Count()
                    SolidElement(
                        Modifier
                            .offset(150.dp, 150.dp)
                            .changeEffect(value, ChangeEffect.strike(Color.Red, 4.dp, hold = 500.milliseconds)),
                    )
                }
            }
        }
    }

    private fun Color.isReddish() = red > 0.6f && blue < 0.6f

    @Test
    fun theLineDrawsAcrossFromTheStartEdge() =
        runEffectTest {
            scene()
            value++
            mainClock.advanceTimeBy(80)
            val early = capture()
            assertTrue(early.colorAt(152, 200).isReddish(), "started at the left")
            assertEquals(Cobalt, early.colorAt(247, 200), "not yet at the right")
            mainClock.advanceTimeBy(300)
            val drawn = capture()
            assertTrue(drawn.colorAt(152, 200).isReddish())
            assertTrue(drawn.colorAt(247, 200).isReddish(), "across the whole element")
            assertEquals(Cobalt, drawn.colorAt(200, 180), "only through the middle")
        }

    @Test
    fun theLineHoldsThenFades() =
        runEffectTest {
            scene()
            value++
            mainClock.advanceTimeBy(700)
            assertTrue(capture().colorAt(200, 200).isReddish(), "still there during the hold")
            mainClock.advanceTimeBy(600)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun rightToLeftLayoutsStrikeFromTheRight() =
        runEffectTest {
            scene(direction = LayoutDirection.Rtl)
            value++
            mainClock.advanceTimeBy(80)
            val early = capture()
            assertTrue(early.colorAt(247, 200).isReddish(), "started at the right")
            assertEquals(Cobalt, early.colorAt(152, 200))
        }

    @Test
    fun reducedMotionShowsTheWholeLineAtOnce() =
        runEffectTest {
            scene(KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(40)
            val image = capture()
            assertTrue(image.colorAt(152, 200).isReddish())
            assertTrue(image.colorAt(247, 200).isReddish())
            mainClock.advanceTimeBy(1200)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun parametersAreChecked() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.strike(width = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.strike(hold = (-1).milliseconds) }
    }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene()
            value++
            mainClock.advanceTimeBy(120)
            onRoot().captureRoboImage("src/jvmTest/screenshots/strike_120.png")
        }
}
