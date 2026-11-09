package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.shineBrushCreations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ShineBrushCostTest {
    private var value by mutableIntStateOf(0)
    private var progress by mutableStateOf(0f)

    @Test
    fun aSweepBuildsItsGradientOnceNotOncePerFrame() =
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, ChangeEffect.shine()))
                }
            }
            mainClock.advanceTimeByFrame()
            val before = shineBrushCreations
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertEquals(1, shineBrushCreations - before, "one gradient for the whole sweep")
        }

    @Test
    fun severalBandsShareOneGradient() =
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, ChangeEffect.shine(bands = 3)))
                }
            }
            mainClock.advanceTimeByFrame()
            val before = shineBrushCreations
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertEquals(1, shineBrushCreations - before)
        }

    @Test
    fun aProgressShineBuildsItsGradientOnce() =
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    SolidElement(Modifier.offset(150.dp, 150.dp).progressEffect(ProgressEffect.shine()) { progress })
                }
            }
            mainClock.advanceTimeByFrame()
            val before = shineBrushCreations
            for (step in 1..60) {
                progress = step / 61f
                mainClock.advanceTimeByFrame()
            }
            assertTrue(shineBrushCreations - before <= 1, "gradients built: ${shineBrushCreations - before}")
        }
}
