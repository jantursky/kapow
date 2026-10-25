package com.jantursky.kapow

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import com.jantursky.kapow.effects.bigHeartGrowth
import com.jantursky.kapow.effects.bigHeartOpacity
import com.jantursky.kapow.effects.bigHeartSize
import com.jantursky.kapow.effects.reducedHeartOpacity
import com.jantursky.kapow.effects.smallHeartsEmission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeartsMathTest {
    private val density = Density(1f)

    @Test
    fun theEmissionHasTheRequestedNumberOfHearts() {
        assertEquals(6, smallHeartsEmission(6, 100f, density).count)
        assertEquals(11, smallHeartsEmission(11, 100f, density).count)
    }

    @Test
    fun theBigHeartGrowsPastFullSizeThenSettles() {
        assertEquals(0f, bigHeartGrowth(0f, 1f), 1e-4f)
        val peak = (0..100).maxOf { bigHeartGrowth(it / 100f, 1f) }
        assertTrue(peak > 1.05f, "overshoots")
        assertEquals(1f, bigHeartGrowth(0.5f, 1f), 1e-4f)
        assertEquals(1f, (0..100).maxOf { bigHeartGrowth(it / 100f, 0f) }, 1e-4f)
    }

    @Test
    fun theBigHeartFadesInTheSecondHalf() {
        assertEquals(1f, bigHeartOpacity(0.3f))
        assertEquals(0.5f, bigHeartOpacity(0.75f), 1e-4f)
        assertEquals(0f, bigHeartOpacity(1f), 1e-4f)
    }

    @Test
    fun theReducedHeartFadesInAndOut() {
        assertEquals(0f, reducedHeartOpacity(0f), 1e-4f)
        assertEquals(1f, reducedHeartOpacity(0.5f), 1e-4f)
        assertEquals(0f, reducedHeartOpacity(1f), 1e-4f)
    }

    @Test
    fun theBigHeartSizeFollowsTheShorterSideWithAFloor() {
        assertEquals(70f, bigHeartSize(Size(300f, 100f), 1f, density), 1e-3f)
        assertEquals(28f, bigHeartSize(Size(20f, 20f), 1f, density), 1e-3f)
        assertEquals(140f, bigHeartSize(Size(300f, 100f), 2f, density), 1e-3f)
    }
}
