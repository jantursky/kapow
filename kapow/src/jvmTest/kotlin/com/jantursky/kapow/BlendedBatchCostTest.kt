package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.particles.paintCreations
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BlendedBatchCostTest {
    private var value by mutableIntStateOf(0)

    private fun sparks() =
        ChangeEffect.spray(
            particles = ParticleContent.shape(ParticleShape.Circle, listOf(Color(0xFFFF8A00)), 16.dp),
            count = 30,
            physics = ParticlePhysics.of(blendMode = BlendMode.Plus),
        )

    @Test
    fun aBlendedBatchMakesOnePaintForSixtyFrames() =
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, sparks()))
                }
            }
            mainClock.advanceTimeByFrame()
            val before = paintCreations
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(paintCreations - before <= 1, "paints made: ${paintCreations - before}")
            assertTrue(paintCreations - before == 1, "the blend is drawn: ${paintCreations - before}")
        }
}
