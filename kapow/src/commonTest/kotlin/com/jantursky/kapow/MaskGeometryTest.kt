package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.jantursky.kapow.transitions.anvilDrop
import com.jantursky.kapow.transitions.anvilShake
import com.jantursky.kapow.transitions.clockSweep
import com.jantursky.kapow.transitions.farthestCorner
import com.jantursky.kapow.transitions.irisRadius
import com.jantursky.kapow.transitions.popScale
import com.jantursky.kapow.transitions.slatOpening
import com.jantursky.kapow.transitions.wipeEdge
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaskGeometryTest {
    private val size = Size(300f, 400f)

    @Test
    fun theIrisReachesTheFarthestCorner() {
        assertEquals(250f, farthestCorner(size, Offset(150f, 200f)))
        assertEquals(500f, farthestCorner(size, Offset.Zero))
        assertEquals(0f, irisRadius(size, Offset(150f, 200f), 0f))
        assertEquals(125f, irisRadius(size, Offset(150f, 200f), 0.5f))
    }

    @Test
    fun theClockClosesJustBeforeTheEnd() {
        assertEquals(0f, clockSweep(0f))
        assertEquals(360f, clockSweep(0.96f))
        assertTrue(clockSweep(0.5f) > 180f)
    }

    @Test
    fun staggeredSlatsOpenInOrder() {
        assertEquals(0.5f, slatOpening(3, 10, 0.5f, staggered = false))
        assertTrue(slatOpening(0, 10, 0.3f, true) > slatOpening(9, 10, 0.3f, true))
        assertEquals(1f, slatOpening(9, 10, 1f, true))
        assertEquals(0f, slatOpening(0, 10, 0f, true))
    }

    @Test
    fun theWipeEdgeCrossesTheWholeElement() {
        assertEquals(-10f, wipeEdge(10f, 0f))
        assertEquals(10f, wipeEdge(10f, 1f))
    }

    @Test
    fun theAnvilFallsThenShakesToRest() {
        assertEquals(1f, anvilDrop(0f))
        assertEquals(0f, anvilDrop(0.1f))
        assertEquals(Offset.Zero, anvilShake(0.05f))
        val end = anvilShake(1f)
        assertTrue(abs(end.x) < 1e-4f && abs(end.y) < 1e-4f)
    }

    @Test
    fun popOvershootsThenSettles() {
        assertEquals(0f, popScale(0f), 1e-4f)
        assertTrue((0..35).any { popScale(it / 100f) > 1f }, "overshoots")
        assertEquals(1f, popScale(0.5f))
    }
}
