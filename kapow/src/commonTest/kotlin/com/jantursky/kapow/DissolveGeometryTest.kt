package com.jantursky.kapow

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.jantursky.kapow.transitions.MAX_CELLS
import com.jantursky.kapow.transitions.cellEdge
import com.jantursky.kapow.transitions.cellGrid
import com.jantursky.kapow.transitions.cellHash
import com.jantursky.kapow.transitions.dissolveShows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DissolveGeometryTest {
    @Test
    fun theHashIsFixedAndSpreadOut() {
        assertEquals(cellHash(3, 4, 5), cellHash(3, 4, 5))
        val values = (0 until 1000).map { cellHash(it % 40, it / 40, 7) }
        assertTrue(values.all { it >= 0f && it < 1f })
        assertTrue(values.count { it < 0.5f } in 400..600, "about half below a half")
    }

    @Test
    fun theGridCoversTheElementAndStaysBounded() {
        assertEquals(IntSize(10, 5), cellGrid(Size(100f, 50f), 10f))
        assertEquals(IntSize(11, 1), cellGrid(Size(101f, 3f), 10f))
        val huge = cellGrid(Size(4000f, 4000f), 2f)
        assertTrue(huge.width * huge.height <= MAX_CELLS + 2 * huge.width)
        assertEquals(0f, cellEdge(0, 3, 100f))
        assertEquals(33f, cellEdge(1, 3, 100f))
        assertEquals(100f, cellEdge(3, 3, 100f))
    }

    @Test
    fun cellsTurnOnOverTheProgress() {
        assertTrue((0 until 10).none { dissolveShows(it, 0, 0f) })
        assertTrue((0 until 10).all { dissolveShows(it, 0, 1f) })
    }
}
