package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProgressEffectsTest {
    private var progress by mutableFloatStateOf(0f)
    private var effect by mutableStateOf(ProgressEffect.iris())

    private fun ComposeUiTest.scene() {
        setEffectContent {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                SolidElement(Modifier.offset(150.dp, 150.dp).progressEffect(effect) { progress })
            }
        }
        mainClock.advanceTimeByFrame()
    }

    private fun ComposeUiTest.at(value: Float) {
        progress = value
        mainClock.advanceTimeByFrame()
    }

    @Test
    fun theIrisRevealsWithProgress() =
        runEffectTest {
            scene()
            assertEquals(Color.White, capture().colorAt(200, 200), "hidden at 0")
            at(0.4f)
            assertEquals(Cobalt, capture().colorAt(200, 200))
            assertEquals(Color.White, capture().colorAt(152, 152))
            at(1f)
            assertEquals(Cobalt, capture().colorAt(152, 152))
        }

    @Test
    fun theIrisFollowsAPlainValue() =
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    // Read here, so the lambda captures a plain Float, as a composable parameter would.
                    val value = progress
                    SolidElement(Modifier.offset(150.dp, 150.dp).progressEffect(ProgressEffect.iris()) { value })
                }
            }
            mainClock.advanceTimeByFrame()
            at(0.4f)
            assertEquals(Cobalt, capture().colorAt(200, 200))
            at(1f)
            assertEquals(Cobalt, capture().colorAt(152, 152))
        }

    @Test
    fun theIrisCanStartFromACorner() =
        runEffectTest {
            effect = ProgressEffect.iris(TransformOrigin(0f, 0f))
            scene()
            at(0.3f)
            assertEquals(Cobalt, capture().colorAt(155, 155))
            assertEquals(Color.White, capture().colorAt(245, 245))
        }

    @Test
    fun theShineCrossesWithProgressAndLeavesNoTrace() =
        runEffectTest {
            effect = ProgressEffect.shine()
            scene()
            assertEquals(Cobalt, capture().colorAt(200, 200))
            at(0.5f)
            assertTrue(capture().colorAt(200, 200).luminance > Cobalt.luminance + 0.05f)
            at(1f)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun swappingTheEffectKeepsTheProgress() =
        runEffectTest {
            scene()
            at(1f)
            effect = ProgressEffect.glow(Color.Red)
            mainClock.advanceTimeByFrame()
            assertTrue(capture().colorAt(258, 200) != Color.White, "a halo at full progress")
        }
}
