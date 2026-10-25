package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.effects.OutlineRoute
import com.jantursky.kapow.effects.glowImpulse
import com.jantursky.kapow.effects.pulseBounds
import com.jantursky.kapow.effects.ringProgress
import com.jantursky.kapow.effects.shineBand
import com.jantursky.kapow.effects.shineLag
import com.jantursky.kapow.effects.shineStops
import com.jantursky.kapow.effects.strikeLine
import com.jantursky.kapow.effects.strikeSegment
import com.jantursky.kapow.effects.strobeProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LightOptionsMathTest {
    private val size = Size(100f, 40f)

    @Test
    fun theDefaultBandIsTheStandardOne() {
        val stops = shineStops(Color.White)
        assertEquals(11, stops.size)
        assertEquals(0f, stops.first().second.alpha)
        assertEquals(0.8f, stops[5].second.alpha, 1e-6f)
        assertEquals(0f, stops.last().second.alpha, 1e-6f)
        assertEquals(shineBand(size, 30f, 0.4f), shineBand(size, 30f, 0.4f, 0.6f, 0f, 0f))
    }

    @Test
    fun aHarderBandReachesItsPeakSoonerAndAPeakScalesIt() {
        val soft = shineStops(Color.White, softness = 1f)
        val hard = shineStops(Color.White, softness = 0.4f)
        assertEquals(21, hard.size)
        // A quarter of the way in, the hard band is already brighter than the soft one.
        assertTrue(hard[5].second.alpha > soft[2].second.alpha)
        assertEquals(0.8f, hard[10].second.alpha, 1e-6f)
        assertEquals(0.3f, shineStops(Color.White, peak = 0.3f)[5].second.alpha, 0.005f)
    }

    @Test
    fun laterBandsTrailTheFirstAndAWiderSweepLetsThemLeave() {
        val first = shineBand(size, 0f, 0.5f)
        val lag = shineLag(size, 0f, 0.6f, 1.5f, 1)
        assertEquals(100f * 0.6f * 1.5f, lag, 1e-3f)
        val second = shineBand(size, 0f, 0.5f, 0.6f, lag)
        assertEquals(first.first.x - lag, second.first.x, 1e-3f)
        // At the end of a two band sweep the last band has left the element.
        val extra = shineLag(size, 0f, 0.6f, 1.5f, 1)
        val last = shineBand(size, 0f, 1f, 0.6f, lag, extra)
        assertTrue(last.first.x >= 100f - 1e-3f, "the second band is past the far edge: ${last.first.x}")
        assertEquals(2 * 5f, shineBand(size, 0f, 0f, 0.1f).second.x - shineBand(size, 0f, 0f, 0.1f).first.x, 1e-3f)
    }

    @Test
    fun aPulseCanStartInsideTheElement() {
        val outside = pulseBounds(size, 0f, 20f)
        assertEquals(0f, outside.left)
        val inside = pulseBounds(size, 0f, 20f, startGrowth = -5f)
        assertEquals(5f, inside.left)
        assertEquals(35f, inside.bottom)
        assertEquals(-20f, pulseBounds(size, 1f, 20f, -5f).left, 1e-4f)
    }

    @Test
    fun glowImpulseIsCappedByTheStackLimit() {
        assertEquals(5f, glowImpulse(false, 0f))
        assertEquals(5f, glowImpulse(true, 4f))
        assertEquals(3.5f, glowImpulse(true, 3f, cap = 3.5f))
        assertEquals(2f, glowImpulse(false, 0f, cap = 2f))
    }

    @Test
    fun ringsStartOneAfterAnotherAndEndTogether() {
        assertEquals(0.3f, ringProgress(0.3f, 0, 1, 0.2f))
        assertEquals(0f, ringProgress(0.3f, 2, 3, 0.2f))
        assertEquals(1f, ringProgress(1f, 2, 3, 0.2f))
        assertEquals(1f, ringProgress(1f, 0, 3, 0.2f))
        assertTrue(ringProgress(0.7f, 1, 3, 0.2f) > ringProgress(0.7f, 2, 3, 0.2f))
    }

    @Test
    fun strobesRepeatTheFlash() {
        assertEquals(0.3f, strobeProgress(0.3f, 1))
        assertEquals(0.5f, strobeProgress(0.25f, 2), 1e-6f)
        assertEquals(0f, strobeProgress(0.5f, 2), 1e-6f)
        assertEquals(0.5f, strobeProgress(0.75f, 2), 1e-6f)
    }

    @Test
    fun theStandardRouteIsOneClockwiseLapFromTheUsualStart() {
        assertTrue(OutlineRoute().isStandard)
        assertFalse(OutlineRoute(laps = 2).isStandard)
        assertFalse(OutlineRoute(startAt = 0.25f).isStandard)
        assertFalse(OutlineRoute(clockwise = false).isStandard)
    }

    @Test
    fun strikeDirectionsRunAcrossTheElement() {
        val reading = strikeSegment(size, 0.5f, StrikeDirection.Reading, LayoutDirection.Ltr)
        assertEquals(strikeLine(size, 0.5f, LayoutDirection.Ltr), reading)
        assertEquals(
            strikeLine(size, 0.5f, LayoutDirection.Rtl),
            strikeSegment(size, 0.5f, StrikeDirection.Reverse, LayoutDirection.Ltr),
        )
        assertEquals(
            strikeLine(size, 0.5f, LayoutDirection.Ltr),
            strikeSegment(size, 0.5f, StrikeDirection.Reverse, LayoutDirection.Rtl),
        )
        assertEquals(
            Offset(50f, 0f) to Offset(50f, 20f),
            strikeSegment(size, 0.5f, StrikeDirection.Vertical, LayoutDirection.Ltr),
        )
        assertEquals(
            Offset(0f, 0f) to Offset(50f, 20f),
            strikeSegment(size, 0.5f, StrikeDirection.Diagonal, LayoutDirection.Ltr),
        )
        assertEquals(
            Offset(100f, 0f) to Offset(50f, 20f),
            strikeSegment(size, 0.5f, StrikeDirection.Diagonal, LayoutDirection.Rtl),
        )
    }

    @Test
    fun strikeOffsetsMoveTheLineAcrossTheElement() {
        val moved = strikeSegment(size, 1f, StrikeDirection.Reading, LayoutDirection.Ltr, offset = 0.25f, shift = 3f)
        assertEquals(20f + 10f + 3f, moved.first.y)
        val vertical = strikeSegment(size, 1f, StrikeDirection.Vertical, LayoutDirection.Ltr, offset = -0.25f)
        assertEquals(25f, vertical.first.x)
        assertEquals(
            strikeLine(size, 1f, LayoutDirection.Ltr),
            strikeSegment(size, 1f, StrikeDirection.Reading, LayoutDirection.Ltr, 0f, 0f),
        )
    }
}
