package com.jantursky.kapow.crease

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jantursky.crease.FoldState
import com.jantursky.crease.Hinge
import com.jantursky.crease.HingeStatus
import com.jantursky.crease.ProvideFoldState
import com.jantursky.kapow.ProgressEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class HingeEffectTest {
    private fun haloPixel(state: FoldState): Color {
        var pixel = Color.Unspecified
        runDesktopComposeUiTest(300, 300) {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    ProvideFoldState(state) {
                        Box(Modifier.fillMaxSize().background(Color.White)) {
                            Box(
                                Modifier
                                    .offset(100.dp, 100.dp)
                                    .size(100.dp)
                                    .hingeEffect(ProgressEffect.glow(Color.Red))
                                    .background(Color.Blue),
                            )
                        }
                    }
                }
            }
            waitForIdle()
            // Just right of the element, inside the halo.
            pixel = onRoot().captureToImage().toPixelMap()[206, 150]
        }
        return pixel
    }

    private fun folded(angle: Float?) = FoldState(Hinge(HingeStatus.PartiallyOpen, angle), emptyList(), emptyList())

    @Test
    fun anOpenHingeDrawsTheEffect() {
        assertNotEquals(Color.White, haloPixel(folded(180f)))
    }

    @Test
    fun aShutHingeDrawsNothing() {
        assertEquals(Color.White, haloPixel(folded(0f)))
    }

    @Test
    fun withoutAHingeAngleTheElementIsUnchanged() {
        assertEquals(Color.White, haloPixel(FoldState.None))
        assertEquals(Color.White, haloPixel(folded(null)))
    }
}
