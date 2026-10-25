package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.transitions.blindsPath
import com.jantursky.kapow.transitions.clockMaskAlpha
import com.jantursky.kapow.transitions.clockPath
import com.jantursky.kapow.transitions.curtainHalfOpening
import com.jantursky.kapow.transitions.shapeIrisSide
import com.jantursky.kapow.transitions.slatOpening
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MaskOptionsGeometryTest {
    @Test
    fun theStaggerAmountSpreadsTheSlatStarts() {
        // 0 opens all slats together, whatever their place.
        for (i in 0 until 10) assertEquals(0.3f, slatOpening(i, 10, 0.3f, true, amount = 0f), 1e-6f)
        // The default amount is the old formula.
        assertEquals(slatOpening(4, 10, 0.4f, true), slatOpening(4, 10, 0.4f, true, amount = 1f))
        // More spread leaves the last slat closed longer.
        assertTrue(slatOpening(9, 10, 0.5f, true, amount = 3f) < slatOpening(9, 10, 0.5f, true, amount = 1f))
        for (amount in listOf(0f, 0.5f, 1f, 4f)) {
            for (i in 0 until 10) assertEquals(1f, slatOpening(i, 10, 1f, true, amount), "slat $i at amount $amount")
            for (i in 0 until 10) assertEquals(0f, slatOpening(i, 10, 0f, true, amount), "slat $i at amount $amount")
        }
    }

    @Test
    fun reversedSlatsOpenFromTheLastOne() {
        assertEquals(slatOpening(0, 10, 0.3f, true), slatOpening(9, 10, 0.3f, true, reverse = true))
        assertEquals(slatOpening(9, 10, 0.3f, true), slatOpening(0, 10, 0.3f, true, reverse = true))
        assertEquals(0.3f, slatOpening(3, 10, 0.3f, staggered = false, reverse = true))
    }

    @Test
    fun aSlatCountSharesTheExtentEvenly() {
        val size = Size(100f, 100f)
        val full = blindsPath(size, 10f, BlindsOrientation.Horizontal, false, 1f, slats = 4)
        assertEquals(0f, full.getBounds().top)
        assertEquals(100f, full.getBounds().bottom)
        // At half opening each of the 4 slats of 25 px is open 12.5 px around its middle.
        val half = blindsPath(size, 10f, BlindsOrientation.Horizontal, false, 0.5f, slats = 4)
        assertEquals(6.25f, half.getBounds().top, 1e-4f)
        assertEquals(100f - 6.25f, half.getBounds().bottom, 1e-4f)
        // Without a count the width decides.
        val byWidth = blindsPath(size, 20f, BlindsOrientation.Vertical, false, 0.5f)
        assertEquals(5f, byWidth.getBounds().left, 1e-4f)
    }

    @Test
    fun aCounterClockwiseSweepRunsTheOtherWay() {
        val size = Size(200f, 200f)
        val cw = clockPath(size, TransformOrigin.Center, 0.2f)
        val ccw = clockPath(size, TransformOrigin.Center, 0.2f, counterClockwise = true)
        assertTrue(cw.getBounds().left >= 99f, "clockwise from 12 o'clock goes right: ${cw.getBounds()}")
        assertTrue(ccw.getBounds().right <= 101f, "counterclockwise goes left: ${ccw.getBounds()}")
        assertEquals(
            clockPath(size, TransformOrigin.Center, 1f).getBounds(),
            clockPath(size, TransformOrigin.Center, 1f, true).getBounds(),
        )
    }

    @Test
    fun aSoftClockEdgeFadesOverItsAngle() {
        assertEquals(1f, clockMaskAlpha(200f, sweep = 100f, softAngle = 20f), "ahead of the edge: hidden")
        assertEquals(0f, clockMaskAlpha(50f, sweep = 100f, softAngle = 20f), "behind the fade: shown")
        assertEquals(1f, clockMaskAlpha(100f, sweep = 100f, softAngle = 20f))
        assertEquals(0.5f, clockMaskAlpha(90f, sweep = 100f, softAngle = 20f))
        assertEquals(
            0f,
            clockMaskAlpha(360f, sweep = 380f, softAngle = 20f),
            "the end of the circle is clear at the end",
        )
    }

    @Test
    fun theCurtainHalvesClearTheirOwnSide() {
        assertEquals(0f, curtainHalfOpening(50f, 3f, 0f))
        assertEquals(53f, curtainHalfOpening(50f, 3f, 1f))
        assertEquals(53f, curtainHalfOpening(50f, 3f, 2f))
        assertEquals(curtainHalfOpening(100f, 3f, 0.5f), 51.5f)
    }

    @Test
    fun theShapeScaleGrowsTheBox() {
        val point = Offset(100f, 100f)
        val size = Size(200f, 200f)
        val one = shapeIrisSide(size, point, 0.5f, 0.5f)
        assertEquals(one * 2, shapeIrisSide(size, point, 0.5f, 0.5f, scale = 2f), 1e-3f)
        assertNotEquals(0f, one)
    }

    @Test
    fun rejectsValuesOutOfRange() {
        assertFailsWith<IllegalArgumentException> { KapowEnter.iris(softEdge = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { KapowExit.clock(softEdge = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.wipe(softEdge = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.glare(intensity = 1.5f) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.glare(width = 0.dp) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.blinds(slatCount = 0) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.blinds(slatCount = 201) }
        assertFailsWith<IllegalArgumentException> { KapowExit.blinds(staggerAmount = -1f) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.curtain(split = 1.2f) }
        assertFailsWith<IllegalArgumentException> { KapowExit.curtain(split = -0.1f) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.shapeIris(scale = 0.5f) }
    }

    @Test
    fun optionsAreWhatMakesTwoTransitionsEqual() {
        assertEquals(KapowEnter.iris(softEdge = 8.dp), KapowEnter.iris(softEdge = 8.dp))
        assertNotEquals(KapowEnter.iris(softEdge = 8.dp), KapowEnter.iris())
        assertNotEquals(KapowEnter.clock(counterClockwise = true), KapowEnter.clock())
        assertNotEquals(KapowEnter.glare(intensity = 0.5f), KapowEnter.glare())
        assertNotEquals(KapowExit.blinds(slatCount = 5), KapowExit.blinds())
        assertNotEquals(KapowExit.curtain(split = 0.3f), KapowExit.curtain())
        assertNotEquals(KapowEnter.shapeIris(rotation = 90f), KapowEnter.shapeIris())
        assertEquals(KapowEnter.iris(), KapowEnter.iris(softEdge = 0.dp))
    }
}
