package com.jantursky.kapow

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.jantursky.kapow.effects.shineBand
import com.jantursky.kapow.effects.shineStops
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShineGeometryTest {
    private val size = Size(200f, 100f)

    @Test
    fun theBandStartsBeforeTheElementAndEndsAfterIt() {
        val (startA, endA) = shineBand(size, angle = 0f, progress = 0f)
        val (startB, endB) = shineBand(size, angle = 0f, progress = 1f)
        assertTrue(endA.x <= 0.01f, "at 0 the band is left of the element: $endA")
        assertTrue(startB.x >= size.width - 0.01f, "at 1 the band is right of the element: $startB")
        assertEquals(startA.y, endA.y)
        assertEquals(startB.y, endB.y)
    }

    @Test
    fun theBandIsCenteredHalfway() {
        val (start, end) = shineBand(size, angle = 45f, progress = 0.5f)
        assertClose(100f, (start.x + end.x) / 2)
        assertClose(50f, (start.y + end.y) / 2)
    }

    @Test
    fun theBandFollowsTheAngle() {
        val (start, end) = shineBand(size, angle = 90f, progress = 0.5f)
        assertClose(start.x, end.x)
        assertTrue(end.y > start.y, "90 degrees sweeps downward")
    }

    @Test
    fun stopsFadeAtTheEdgesAndPeakInTheMiddle() {
        val stops = shineStops(Color.White)
        assertEquals(0f, stops.first().first)
        assertEquals(1f, stops.last().first)
        assertTrue(stops.first().second.alpha < 0.01f)
        assertTrue(stops.last().second.alpha < 0.01f)
        assertClose(0.8f, stops[stops.size / 2].second.alpha)
    }

    private fun assertClose(
        expected: Float,
        actual: Float,
    ) = assertTrue(abs(expected - actual) < 0.01f, "expected $expected, got $actual")
}
