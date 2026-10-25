package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class TypewriterTest {
    private var text by mutableStateOf("The quick brown fox")
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        motion: KapowMotion = KapowMotion.Full,
        cursor: TypewriterCursor? = null,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                KapowTypewriter(
                    text,
                    Modifier.testTag("tw"),
                    style = TextStyle(color = Color.Black, fontSize = 24.sp),
                    charsPerSecond = 20f,
                    cursor = cursor,
                )
            }
        }
    }

    private fun ImageBitmap.darkPixels(): Int {
        var n = 0
        for (x in 0 until width) for (y in 0 until height / 2) if (colorAt(x, y).luminance < 0.5f) n++
        return n
    }

    private fun ComposeUiTest.size(): IntSize = onNodeWithTag("tw").fetchSemanticsNode().size

    @Test
    fun theLayoutHasItsFinalSizeFromTheFirstFrame() =
        runEffectTest {
            scene()
            val first = size()
            assertTrue(first.width > 100 && first.height > 10)
            mainClock.advanceTimeBy(300)
            assertEquals(first, size())
            mainClock.advanceTimeBy(2000)
            assertEquals(first, size())
        }

    @Test
    fun charactersAppearOverTime() =
        runEffectTest {
            scene()
            mainClock.advanceTimeBy(40)
            val start = capture().darkPixels()
            mainClock.advanceTimeBy(500)
            val middle = capture().darkPixels()
            mainClock.advanceTimeBy(3000)
            val end = capture().darkPixels()
            assertTrue(start < middle, "typing started")
            assertTrue(middle < end, "typing continued to the end")
        }

    @Test
    fun aNewTextRestartsTheReveal() =
        runEffectTest {
            scene()
            mainClock.advanceTimeBy(3000)
            val full = capture().darkPixels()
            text = "Jumps over the lazy dog"
            mainClock.advanceTimeByFrame()
            assertTrue(capture().darkPixels() < full / 2, "restarted from nothing")
            mainClock.advanceTimeBy(3000)
            assertTrue(capture().darkPixels() > full / 2)
        }

    @Test
    fun reducedMotionShowsEverythingAtOnce() =
        runEffectTest {
            scene(KapowMotion.Reduced, cursor = TypewriterCursor.bar())
            mainClock.advanceTimeByFrame()
            val shown = capture().darkPixels()
            assertTrue(shown > 100)
            mainClock.advanceTimeBy(2000)
            assertEquals(shown, capture().darkPixels(), "no cursor, nothing changes")
        }

    @Test
    fun accessibilityReadsTheFullText() =
        runEffectTest {
            scene()
            mainClock.advanceTimeBy(100)
            onNodeWithTag("tw").assertTextEquals("The quick brown fox")
        }

    @Test
    fun theCursorBlinksAfterTheTextIsComplete() =
        runEffectTest {
            scene(cursor = TypewriterCursor.bar(blink = 500.milliseconds))
            mainClock.advanceTimeBy(2200)
            val a = capture().darkPixels()
            mainClock.advanceTimeBy(500)
            val b = capture().darkPixels()
            assertTrue(a != b, "the bar toggles")
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            text = "Something else"
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun parametersAreChecked() {
        assertFailsWith<IllegalArgumentException> { TypewriterCursor.bar(blink = 0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { TypewriterCursor.bar(width = Dp.Hairline) }
    }
}
