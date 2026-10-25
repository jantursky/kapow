package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class CounterTest {
    private var value by mutableLongStateOf(0L)
    private val counter = CompositionCounter()
    private val formatted = mutableListOf<Long>()

    private fun ComposeUiTest.scene(
        motion: KapowMotion = KapowMotion.Full,
        mode: CounterMode = CounterMode.Count,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                KapowCounter(
                    value,
                    Modifier.testTag("counter"),
                    style = TextStyle(color = Color.Black, fontSize = 32.sp),
                    mode = mode,
                    format = {
                        formatted += it
                        it.toString()
                    },
                    duration = 500.milliseconds,
                )
            }
        }
    }

    private fun ComposeUiTest.size(): IntSize = onNodeWithTag("counter").fetchSemanticsNode().size

    private fun ImageBitmap.darkPixels(): Int {
        var n = 0
        for (x in 0 until width) for (y in 0 until height / 2) if (colorAt(x, y).luminance < 0.5f) n++
        return n
    }

    @Test
    fun theFirstCompositionShowsTheValueWithoutAnimating() =
        runEffectTest {
            value = 1234
            scene()
            onNodeWithTag("counter").assertTextEquals("1234")
            val start = capture().darkPixels()
            assertTrue(start > 50)
            mainClock.advanceTimeBy(1000)
            assertEquals(start, capture().darkPixels())
            assertEquals(setOf(1234L), formatted.toSet())
        }

    @Test
    fun countingGoesUpMonotonicallyAndEndsExactly() =
        runEffectTest {
            scene()
            mainClock.advanceTimeByFrame()
            formatted.clear()
            value = 1000
            repeat(60) { mainClock.advanceTimeByFrame() }
            val between = formatted.filter { it in 1..999 }
            assertTrue(between.isNotEmpty(), "counted through values in between")
            assertEquals(between.sorted(), between)
            assertEquals(1000L, formatted.last())
        }

    @Test
    fun countingGoesDownToo() =
        runEffectTest {
            value = 900
            scene()
            mainClock.advanceTimeByFrame()
            formatted.clear()
            value = 100
            repeat(60) { mainClock.advanceTimeByFrame() }
            val between = formatted.filter { it in 101..899 }
            assertTrue(between.isNotEmpty())
            assertEquals(between.sortedDescending(), between)
            assertEquals(100L, formatted.last())
        }

    @Test
    fun accessibilityReadsTheTargetAtOnce() =
        runEffectTest {
            scene()
            value = 5000
            mainClock.advanceTimeByFrame()
            onNodeWithTag("counter").assertTextEquals("5000")
        }

    @Test
    fun theWidthFitsTheWiderOfOldAndNewWhileMoving() =
        runEffectTest {
            value = 9
            scene()
            val narrow = size()
            value = 10000
            mainClock.advanceTimeByFrame()
            val wide = size()
            assertTrue(wide.width > narrow.width)
            mainClock.advanceTimeBy(200)
            assertEquals(wide, size())
            mainClock.advanceTimeBy(1000)
            assertEquals(wide, size(), "the new text is the wider one")
        }

    @Test
    fun theWidthSettlesOnTheNewTextWhenItIsNarrower() =
        runEffectTest {
            value = 10000
            scene()
            val wide = size()
            value = 9
            mainClock.advanceTimeByFrame()
            assertEquals(wide, size())
            mainClock.advanceTimeBy(1000)
            assertTrue(size().width < wide.width)
        }

    @Test
    fun reducedMotionJumpsToTheNewValue() =
        runEffectTest {
            scene(KapowMotion.Reduced)
            mainClock.advanceTimeByFrame()
            formatted.clear()
            value = 1000
            repeat(10) { mainClock.advanceTimeByFrame() }
            assertEquals(setOf(1000L), formatted.toSet())
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value = 4321
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun parametersAreChecked() =
        runEffectTest {
            assertFailsWith<IllegalArgumentException> {
                setEffectContent { KapowCounter(1, duration = (-1).milliseconds) }
            }
        }
}
