package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.jantursky.kapow.transitions.encloses
import com.jantursky.kapow.transitions.inradius
import com.jantursky.kapow.transitions.shapeIrisSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShapeIrisGeometryTest {
    @Test
    fun theInradiusIsTheNearestSide() {
        val xs = floatArrayOf(0f, 100f, 100f, 0f)
        val ys = floatArrayOf(0f, 0f, 60f, 60f)
        assertEquals(30f, inradius(xs, ys, Offset(50f, 30f)))
        assertEquals(10f, inradius(xs, ys, Offset(10f, 30f)))
        assertEquals(0f, inradius(xs, ys, Offset(150f, 30f)), "outside")
        assertTrue(encloses(xs, ys, Offset(99f, 59f)))
        assertFalse(encloses(xs, ys, Offset(-1f, 30f)))
    }

    @Test
    fun theBoxGrowsUntilTheHeldCircleReachesTheFarthestCorner() {
        val size = Size(300f, 400f)
        val center = Offset(150f, 200f)
        assertEquals(0f, shapeIrisSide(size, center, 0.5f, 0f))
        assertEquals(500f, shapeIrisSide(size, center, 0.5f, 1f))
        assertEquals(1000f, shapeIrisSide(size, center, 0.25f, 1f))
        assertEquals(500f, shapeIrisSide(size, center, 0.25f, 0.5f))
    }
}
