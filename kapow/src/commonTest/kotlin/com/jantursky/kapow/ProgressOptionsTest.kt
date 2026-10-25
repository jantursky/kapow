package com.jantursky.kapow

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.fillRotation
import com.jantursky.kapow.effects.fillSurface
import com.jantursky.kapow.effects.progressShineBand
import com.jantursky.kapow.effects.progressShineStops
import com.jantursky.kapow.effects.rampColor
import com.jantursky.kapow.effects.shineBand
import com.jantursky.kapow.effects.shineStops
import com.jantursky.kapow.effects.softIrisRadius
import com.jantursky.kapow.effects.traceStretch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ProgressOptionsTest {
    private val size = Size(200f, 100f)

    @Test
    fun oneBandOfTheDefaultWidthIsTheOldBand() {
        for (angle in listOf(0f, 45f, 90f, 135f)) {
            for (progress in listOf(0f, 0.3f, 0.5f, 1f)) {
                assertEquals(
                    shineBand(size, angle, progress),
                    progressShineBand(size, angle, progress, 0.6f, 0.3f, 0, 1),
                )
            }
        }
    }

    @Test
    fun aWiderBandCoversMoreOfTheSweep() {
        val (narrowStart, narrowEnd) = progressShineBand(size, 0f, 0.5f, 0.3f, 0.3f, 0, 1)
        val (wideStart, wideEnd) = progressShineBand(size, 0f, 0.5f, 0.9f, 0.3f, 0, 1)
        assertTrue((wideEnd - wideStart).getDistance() > (narrowEnd - narrowStart).getDistance() * 2.5f)
    }

    @Test
    fun severalBandsAreSpacedAlongTheSweepAndStartAndEndOutside() {
        val centers =
            (0 until 3).map { progressShineBand(size, 0f, 0.5f, 0.2f, 0.4f, it, 3) }.map { (a, b) ->
                (
                    a.x +
                        b.x
                ) /
                    2
            }
        assertEquals(80f, centers[1] - centers[0], 0.01f)
        assertEquals(80f, centers[2] - centers[1], 0.01f)
        for (index in 0 until 3) {
            val (a, b) = progressShineBand(size, 0f, 0f, 0.2f, 0.4f, index, 3)
            assertTrue(maxOf(a.x, b.x) <= 0f, "band $index starts left of the element")
            val (c, d) = progressShineBand(size, 0f, 1f, 0.2f, 0.4f, index, 3)
            assertTrue(minOf(c.x, d.x) >= size.width, "band $index ends right of the element")
        }
    }

    @Test
    fun softnessSpreadsOrTightensTheBand() {
        assertTrue(progressShineStops(Color.White, 1f).contentEquals(shineStops(Color.White)))
        val usual = progressShineStops(Color.White, 1f)[2].second.alpha
        assertTrue(progressShineStops(Color.White, 2f)[2].second.alpha > usual)
        assertTrue(progressShineStops(Color.White, 0.5f)[2].second.alpha < usual)
        assertEquals(0.8f, progressShineStops(Color.White, 2f)[5].second.alpha, 1e-5f)
    }

    @Test
    fun aRampBlendsAlongItsColors() {
        val ramp = listOf(Color.Red, Color.Green, Color.Blue)
        assertEquals(Color.Red, rampColor(ramp, 0f))
        assertEquals(Color.Green, rampColor(ramp, 0.5f))
        assertEquals(Color.Blue, rampColor(ramp, 1f))
        assertTrue(rampColor(ramp, 0.25f).let { it.red > 0.4f && it.green > 0.4f })
        assertEquals(Color.Red, rampColor(listOf(Color.Red), 0.7f))
    }

    @Test
    fun aSoftIrisIsEmptyAtZeroAndSolidWhereItEndsAtOne() {
        assertEquals(0f, softIrisRadius(100f, 20f, 0f))
        assertEquals(120f, softIrisRadius(100f, 20f, 1f))
        // At 1 the solid part, a soft edge inside the radius, still reaches the farthest corner.
        assertEquals(100f, softIrisRadius(100f, 20f, 1f) - 20f)
        assertEquals(50f, softIrisRadius(100f, 0f, 0.5f))
    }

    @Test
    fun theLiquidCanHaveMoreWavesAndRollMoreOrLess() {
        val default = fillSurface(30f, 100f, 100f, 0.5f, 8f)
        assertEquals(default, fillSurface(30f, 100f, 100f, 0.5f, 8f, 0f, 1.5f, 3f))
        assertNotEquals(default, fillSurface(30f, 100f, 100f, 0.5f, 8f, 0f, 4f, 3f))
        val still = fillSurface(30f, 100f, 100f, 0.4f, 8f, 0f, 1.5f, 0f)
        val stillLater = fillSurface(30f, 100f, 100f, 0.6f, 8f, 0f, 1.5f, 0f)
        // With no roll only the level moves: the wave keeps the same offset from it.
        assertEquals(still - 60f, stillLater - 40f, 1e-3f)
    }

    @Test
    fun everyDirectionTurnsTheUpwardFill() {
        assertEquals(0f, fillRotation(FillDirection.Up))
        assertEquals(180f, fillRotation(FillDirection.Down))
        assertEquals(90f, fillRotation(FillDirection.Right))
        assertEquals(-90f, fillRotation(FillDirection.Left))
    }

    private fun assertStretch(
        expected: Pair<Float, Float>,
        actual: Pair<Float, Float>,
    ) {
        assertEquals(expected.first, actual.first, 1e-3f)
        assertEquals(expected.second, actual.second, 1e-3f)
    }

    @Test
    fun aTraceStartsWhereItIsToldAndGoesBothWays() {
        assertStretch(0f to 25f, traceStretch(100f, 0.25f, 0f, true, 1f))
        assertStretch(-25f to 0f, traceStretch(100f, 0.25f, 0f, false, 1f))
        assertStretch(50f to 75f, traceStretch(100f, 0.25f, 0.5f, true, 1f))
        assertStretch(25f to 50f, traceStretch(100f, 0.25f, 0.5f, false, 1f))
    }

    @Test
    fun aShortTailLeavesOnlyTheHeadDrawn() {
        assertStretch(0f to 20f, traceStretch(100f, 0.2f, 0f, true, 0.5f))
        assertStretch(40f to 60f, traceStretch(100f, 0.6f, 0f, true, 0.2f))
        assertStretch(-60f to -40f, traceStretch(100f, 0.6f, 0f, false, 0.2f))
        assertStretch(0f to 100f, traceStretch(100f, 1f, 0f, true, 1f))
    }

    @Test
    fun equalOptionsGiveEqualEffects() {
        assertEquals(ProgressEffect.shine(bands = 2), ProgressEffect.shine(bands = 2))
        assertNotEquals(ProgressEffect.shine(bands = 2), ProgressEffect.shine(bands = 3))
        assertEquals(
            ProgressEffect.shine(blendMode = BlendMode.Screen),
            ProgressEffect.shine(blendMode = BlendMode.Screen),
        )
        assertEquals(
            ProgressEffect.glow(alphaCurve = FastOutSlowInEasing),
            ProgressEffect.glow(alphaCurve = FastOutSlowInEasing),
        )
        assertEquals(
            ProgressEffect.iris(softEdge = 8.dp, shape = CircleShape),
            ProgressEffect.iris(softEdge = 8.dp, shape = CircleShape),
        )
        assertEquals(
            ProgressEffect.fill(direction = FillDirection.Left),
            ProgressEffect.fill(direction = FillDirection.Left),
        )
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        assertEquals(ProgressEffect.trace(brush = brush), ProgressEffect.trace(brush = brush))
        assertNotEquals(ProgressEffect.trace(start = 0.5f), ProgressEffect.trace())
    }

    @Test
    fun theFactoriesCheckTheNewOptions() {
        assertFailsWith<IllegalArgumentException> { ProgressEffect.shine(bandWidth = 0f) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.shine(bandWidth = 2f) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.shine(softness = 0f) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.shine(bands = 0) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.shine(bandSpacing = -1f) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.glow(colorRamp = emptyList()) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.iris(softEdge = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.fill(waves = 0f) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.fill(colorRamp = emptyList()) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.fill(foam = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.trace(start = 1.5f) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.trace(tail = 0f) }
        assertFailsWith<IllegalArgumentException> { ProgressEffect.trace(tail = 1.1f) }
    }
}
