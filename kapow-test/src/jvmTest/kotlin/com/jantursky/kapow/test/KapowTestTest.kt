package com.jantursky.kapow.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.KapowTestApi
import com.jantursky.kapow.LocalKapowEffects
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.isReducedMotion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class, KapowTestApi::class)
class KapowTestTest {
    private var value by mutableIntStateOf(0)

    @Composable
    private fun Box100(tag: String) {
        Box(
            Modifier
                .testTag(tag)
                .size(100.dp)
                .changeEffect(value, ChangeEffect.shine(angle = 0f, duration = 1.seconds))
                .background(Color(0xFF3D7BFF)),
        )
    }

    private fun ImageBitmap.row(): List<Color> = toPixelMap().let { map -> (0 until 100).map { map[it, 50] } }

    @Test
    fun disabledDrawsNothing() =
        runDesktopComposeUiTest(200, 200) {
            setKapowContent { Box100("a") }
            val before = onNodeWithTag("a").captureToImage().row()
            value++
            advanceKapowEffects(500.milliseconds)
            assertEquals(before, onNodeWithTag("a").captureToImage().row())
        }

    @Test
    fun fullPlaysTheEffect() =
        runDesktopComposeUiTest(200, 200) {
            setKapowContent(KapowTestMode.Full) { Box100("a") }
            val before = onNodeWithTag("a").captureToImage().row()
            value++
            advanceKapowEffects(500.milliseconds)
            assertNotEquals(before, onNodeWithTag("a").captureToImage().row())
        }

    @Test
    fun reducedAndFullOverrideThePlatform() =
        runDesktopComposeUiTest(200, 200) {
            val seen = mutableListOf<Boolean>()
            setKapowContent(KapowTestMode.Reduced) { seen += isReducedMotion() }
            assertTrue(seen.last())
        }

    @Test
    fun fullIsNotReduced() =
        runDesktopComposeUiTest(200, 200) {
            val seen = mutableListOf<Boolean>()
            setKapowContent(KapowTestMode.Full) { seen += isReducedMotion() }
            assertEquals(false, seen.last())
        }

    @Test
    fun sameStartAndStepGiveSamePixels() {
        fun frame(): List<Color> {
            var row: List<Color> = emptyList()
            value = 0
            runDesktopComposeUiTest(200, 200) {
                setKapowContent(KapowTestMode.Full) { Box100("a") }
                value++
                advanceKapowEffects(400.milliseconds)
                row = onNodeWithTag("a").captureToImage().row()
            }
            return row
        }
        assertEquals(frame(), frame())
    }

    @Test
    fun siblingsBothObeyTheScope() =
        runDesktopComposeUiTest(300, 200) {
            setKapowContent {
                Row {
                    Box100("a")
                    Box100("b")
                }
            }
            val before = listOf("a", "b").map { onNodeWithTag(it).captureToImage().row() }
            value++
            advanceKapowEffects(500.milliseconds)
            assertEquals(before, listOf("a", "b").map { onNodeWithTag(it).captureToImage().row() })
        }

    @Test
    fun theComposableDefaultsToDisabled() =
        runDesktopComposeUiTest(200, 200) {
            var effects = true
            setContent { KapowTest { effects = LocalKapowEffects.current } }
            assertEquals(false, effects)
        }
}
