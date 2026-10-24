package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.jantursky.kapow.transitions.BLOB_POINTS
import com.jantursky.kapow.transitions.BLOB_WOBBLE
import com.jantursky.kapow.transitions.blobFactors
import com.jantursky.kapow.transitions.blobReach
import com.jantursky.kapow.transitions.farthestCorner
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlobGeometryTest {
    @Test
    fun everyPointWobblesWithinItsRange() {
        for (step in 0..20) {
            val factors = blobFactors(step / 20f)
            assertEquals(BLOB_POINTS, factors.size)
            assertTrue(factors.all { it in (1 - BLOB_WOBBLE)..(1 + BLOB_WOBBLE) })
        }
        assertTrue(blobFactors(0.5f).toSet().size > 1, "the points differ")
        assertContentEquals(blobFactors(0.4f), blobFactors(0.4f))
    }

    @Test
    fun theFullBlobCoversTheFarthestCornerEvenAtItsNarrowest() {
        val size = Size(300f, 200f)
        val point = Offset(0f, 200f)
        val inset = cos(PI.toFloat() / BLOB_POINTS)
        val narrowest = blobReach(size, point) * (1 - BLOB_WOBBLE) * inset * inset
        assertEquals(farthestCorner(size, point), narrowest, 1e-3f)
    }
}
