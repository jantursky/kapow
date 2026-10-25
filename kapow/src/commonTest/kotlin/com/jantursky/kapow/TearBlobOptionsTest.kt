package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.transitions.blobFactors
import com.jantursky.kapow.transitions.blobReach
import com.jantursky.kapow.transitions.farthestCorner
import com.jantursky.kapow.transitions.tearEdge
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TearBlobOptionsTest {
    private fun FloatArray.xs(): List<Float> = filterIndexed { i, _ -> i % 2 == 0 }

    /** How much the edge zigzags: the sum of the steps between neighbouring points, sideways. */
    private fun FloatArray.zigzag(): Float = xs().zipWithNext { a, b -> abs(b - a) }.sum()

    @Test
    fun theDefaultRoughnessIsTheOldTear() {
        assertContentEquals(tearEdge(100f, 10f, 0.5f), tearEdge(100f, 10f, 0.5f, roughness = 0.4f))
    }

    @Test
    fun aRougherTearZigzagsMore() {
        val smooth = tearEdge(100f, 10f, 0.5f, roughness = 0f)
        val rough = tearEdge(100f, 10f, 0.5f, roughness = 1f)
        assertTrue(rough.zigzag() > smooth.zigzag() * 2, "${rough.zigzag()} against ${smooth.zigzag()}")
    }

    @Test
    fun everyRoughnessClearsTheContentAtBothEnds() {
        for (roughness in listOf(0f, 0.2f, 0.4f, 0.8f, 1f)) {
            assertTrue(tearEdge(100f, 10f, 0f, roughness).xs().all { it <= -100f }, "start at $roughness")
            assertTrue(tearEdge(100f, 10f, 1f, roughness).xs().all { it >= 100f }, "end at $roughness")
        }
    }

    @Test
    fun theBlobHasTheGivenNumberOfPointsAndWobble() {
        for (count in listOf(4, 5, 9, 20)) {
            for (wobble in listOf(0f, 0.1f, 0.5f)) {
                val factors = blobFactors(0.37f, count, wobble)
                assertEquals(count, factors.size)
                assertTrue(factors.all { it in (1 - wobble - 1e-5f)..(1 + wobble + 1e-5f) }, "$count points, $wobble")
            }
        }
        assertTrue(blobFactors(0.4f, 9, 0f).all { it == 1f }, "no wobble, a circle")
    }

    @Test
    fun theBlobSeedChangesTheWobble() {
        assertContentEquals(blobFactors(0.4f, 9, 0.25f, 5), blobFactors(0.4f, 9, 0.25f, 5))
        assertTrue(!blobFactors(0.4f, 9, 0.25f, 5).contentEquals(blobFactors(0.4f, 9, 0.25f, 6)))
    }

    @Test
    fun theBlobStillCoversTheCornersWhateverItsShape() {
        val size = Size(300f, 200f)
        val point = Offset(150f, 100f)
        for (count in listOf(4, 5, 9, 32)) {
            for (wobble in listOf(0f, 0.25f, 0.5f)) {
                val inset = cos(PI.toFloat() / count)
                val narrowest = blobReach(size, point, count, wobble) * (1 - wobble) * inset * inset
                assertTrue(narrowest >= farthestCorner(size, point) - 1e-2f, "$count points, $wobble")
            }
        }
    }

    @Test
    fun rejectsValuesOutOfRange() {
        assertFailsWith<IllegalArgumentException> { KapowEnter.tear(roughness = 1.5f) }
        assertFailsWith<IllegalArgumentException> { KapowExit.tear(roughness = -0.1f) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.blob(points = 3) }
        assertFailsWith<IllegalArgumentException> { KapowExit.blob(points = 33) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.blob(wobble = 0.6f) }
        assertFailsWith<IllegalArgumentException> { KapowExit.blob(wobble = -0.1f) }
    }

    @Test
    fun optionsAreWhatMakesTwoTransitionsEqual() {
        assertEquals(KapowEnter.tear(), KapowEnter.tear(roughness = 0.4f, rimColor = Color.White.copy(alpha = 0.75f)))
        assertNotEquals(KapowEnter.tear(), KapowEnter.tear(roughness = 0.6f))
        assertNotEquals(KapowExit.tear(), KapowExit.tear(rimColor = Color.Red))
        assertEquals(KapowEnter.blob(), KapowEnter.blob(points = 9, wobble = 0.25f, seed = 53))
        assertNotEquals(KapowEnter.blob(), KapowEnter.blob(points = 7))
        assertNotEquals(KapowExit.blob(), KapowExit.blob(wobble = 0.1f))
        assertNotEquals(KapowExit.blob(), KapowExit.blob(seed = 1))
        assertEquals(KapowEnter.tear(toothSize = 12.dp), KapowEnter.tear())
    }
}
