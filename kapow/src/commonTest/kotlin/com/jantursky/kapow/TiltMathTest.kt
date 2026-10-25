package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.jantursky.kapow.tilt.TiltAngles
import com.jantursky.kapow.tilt.normalizedPointer
import com.jantursky.kapow.tilt.sheenCenter
import com.jantursky.kapow.tilt.tiltAngles
import com.jantursky.kapow.tilt.tiltStrength
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TiltMathTest {
    private val size = Size(200f, 100f)

    @Test
    fun thePointerIsNormalizedToMinusOneToOne() {
        assertEquals(Offset(-1f, -1f), normalizedPointer(Offset(0f, 0f), size))
        assertEquals(Offset(1f, 1f), normalizedPointer(Offset(200f, 100f), size))
        assertEquals(Offset(0f, 0f), normalizedPointer(Offset(100f, 50f), size))
        assertEquals(Offset(0.5f, -0.5f), normalizedPointer(Offset(150f, 25f), size))
    }

    @Test
    fun aPointerOutsideCountsAsOnTheEdge() {
        assertEquals(Offset(1f, -1f), normalizedPointer(Offset(900f, -40f), size))
        assertEquals(Offset.Zero, normalizedPointer(Offset(10f, 10f), Size.Zero))
    }

    @Test
    fun theMiddleIsFlat() {
        assertEquals(TiltAngles.Flat, tiltAngles(Offset(100f, 50f), size, 12f))
    }

    @Test
    fun theTopRightTiltsByTheMaximumAndTheSideOfThePointerDips() {
        val angles = tiltAngles(Offset(200f, 0f), size, 12f)
        assertEquals(12f, angles.rotationY)
        assertEquals(12f, angles.rotationX)
        val bottomLeft = tiltAngles(Offset(0f, 100f), size, 12f)
        assertEquals(-12f, bottomLeft.rotationY)
        assertEquals(-12f, bottomLeft.rotationX)
    }

    @Test
    fun theAngleGrowsWithTheDistanceFromTheMiddleAndNeverPassesTheMaximum() {
        val half = tiltAngles(Offset(150f, 50f), size, 12f)
        assertEquals(6f, half.rotationY)
        assertEquals(0f, half.rotationX)
        for (x in -50..250 step 10) {
            for (y in -50..150 step 10) {
                val a = tiltAngles(Offset(x.toFloat(), y.toFloat()), size, 12f)
                assertTrue(kotlin.math.abs(a.rotationX) <= 12f && kotlin.math.abs(a.rotationY) <= 12f)
            }
        }
    }

    @Test
    fun theStrengthIsZeroWhenFlatAndOneAtTheMaximum() {
        assertEquals(0f, tiltStrength(TiltAngles.Flat, 12f))
        assertEquals(1f, tiltStrength(TiltAngles(12f, 0f), 12f))
        assertEquals(1f, tiltStrength(TiltAngles(12f, 12f), 12f))
        assertEquals(0.5f, tiltStrength(TiltAngles(0f, 6f), 12f))
        assertEquals(0f, tiltStrength(TiltAngles(3f, 3f), 0f))
    }

    @Test
    fun theSheenIsMirroredFromThePointer() {
        // A pointer at the top right gives the maximum angles, and puts the light at the bottom left.
        val pointer = tiltAngles(Offset(200f, 0f), size, 12f)
        assertEquals(Offset(0f, 100f), sheenCenter(pointer, 12f, size))
        assertEquals(Offset(100f, 50f), sheenCenter(TiltAngles.Flat, 12f, size))
        assertEquals(Offset(100f, 50f), sheenCenter(pointer, 0f, size))
    }
}
