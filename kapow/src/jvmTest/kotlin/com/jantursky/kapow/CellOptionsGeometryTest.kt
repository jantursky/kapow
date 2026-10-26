package com.jantursky.kapow

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.transitions.CHECKER_WAVE
import com.jantursky.kapow.transitions.cellGrowth
import com.jantursky.kapow.transitions.checkerStart
import com.jantursky.kapow.transitions.dissolveShown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CellOptionsGeometryTest {
    @Test
    fun aCellPopsWithoutSoftnessAndGrowsWithIt() {
        assertEquals(0f, cellGrowth(0.4f, 0.3f, 0f))
        assertEquals(1f, cellGrowth(0.4f, 0.5f, 0f))
        assertEquals(0f, cellGrowth(0.4f, 0f, 0.5f))
        assertTrue(cellGrowth(0.4f, 0.3f, 0.5f) in 0.01f..0.99f, "mid growth")
        // Every cell is whole at the end, however late its moment.
        for (softness in listOf(0.1f, 0.5f, 1f)) assertEquals(1f, cellGrowth(0.999f, 1f, softness))
        // With full softness all cells grow together.
        assertEquals(cellGrowth(0.1f, 0.5f, 1f), cellGrowth(0.9f, 0.5f, 1f))
    }

    @Test
    fun theShownCellsGrowWithTheProgress() {
        val size = Size(100f, 100f)
        for (shape in CellShape.entries) {
            val none = dissolveShown(size, 10f, 0f, shape, 0.5f, 1)
            val some = dissolveShown(size, 10f, 0.5f, shape, 0.5f, 1)
            val all = dissolveShown(size, 10f, 1f, shape, 0.5f, 1)
            assertTrue(none.isEmpty, "$shape: nothing at 0")
            assertTrue(!some.isEmpty, "$shape: something half way")
            val bounds = all.getBounds()
            assertTrue(bounds.left <= 0.5f && bounds.top <= 0.5f, "$shape covers the top left: $bounds")
            assertTrue(bounds.right >= 99.5f && bounds.bottom >= 99.5f, "$shape covers the bottom right: $bounds")
        }
    }

    @Test
    fun theSeedChangesTheOrder() {
        val size = Size(100f, 100f)
        val a = dissolveShown(size, 10f, 0.4f, CellShape.Square, 0f, 1).getBounds()
        val sameSeed = dissolveShown(size, 10f, 0.4f, CellShape.Square, 0f, 1).getBounds()
        assertEquals(a, sameSeed)
        val shapes = (1..6).map { dissolveShown(size, 10f, 0.2f, CellShape.Circle, 0f, it).getBounds() }
        assertTrue(shapes.toSet().size > 1, "other seeds show other cells")
    }

    @Test
    fun checkerOrdersSpreadTheStarts() {
        val span = 1f - CHECKER_WAVE
        // The waves: the two colors start apart and nothing else is spread.
        assertEquals(0f, checkerStart(0, 0, 5, 5, CheckerOrder.Together, 1))
        assertEquals(span, checkerStart(1, 0, 5, 5, CheckerOrder.Together, 1))
        // A diagonal sweep runs from the first corner to the last.
        assertEquals(0f, checkerStart(0, 0, 5, 5, CheckerOrder.Diagonal, 1))
        assertEquals(span, checkerStart(4, 4, 5, 5, CheckerOrder.Diagonal, 1), 1e-6f)
        assertTrue(
            checkerStart(1, 2, 5, 5, CheckerOrder.Diagonal, 1) < checkerStart(3, 2, 5, 5, CheckerOrder.Diagonal, 1),
        )
        // The spiral starts in the middle and ends at the rim.
        assertTrue(checkerStart(2, 2, 5, 5, CheckerOrder.Spiral, 1) < checkerStart(0, 0, 5, 5, CheckerOrder.Spiral, 1))
        assertTrue(checkerStart(2, 2, 5, 5, CheckerOrder.Spiral, 1) < checkerStart(1, 2, 5, 5, CheckerOrder.Spiral, 1))
        // Every start leaves room to finish.
        for (order in CheckerOrder.entries) {
            for (c in 0 until 7) {
                for (r in 0 until 4) {
                    val start = checkerStart(c, r, 7, 4, order, 3)
                    assertTrue(start >= 0f && start <= span + 1e-6f, "$order $c,$r: $start")
                }
            }
        }
    }

    @Test
    fun randomStartsFollowTheSeed() {
        val a = (0 until 20).map { checkerStart(it, 0, 20, 1, CheckerOrder.Random, 1) }
        assertEquals(a, (0 until 20).map { checkerStart(it, 0, 20, 1, CheckerOrder.Random, 1) })
        assertNotEquals(a, (0 until 20).map { checkerStart(it, 0, 20, 1, CheckerOrder.Random, 2) })
    }

    @Test
    fun rejectsSoftnessOutOfRange() {
        assertFailsWith<IllegalArgumentException> { KapowEnter.dissolve(softness = 1.5f) }
        assertFailsWith<IllegalArgumentException> { KapowExit.dissolve(softness = -0.1f) }
    }

    @Test
    fun optionsAreWhatMakesTwoTransitionsEqual() {
        assertEquals(KapowEnter.dissolve(), KapowEnter.dissolve(cellShape = CellShape.Square, softness = 0f))
        assertNotEquals(KapowEnter.dissolve(), KapowEnter.dissolve(cellShape = CellShape.Hexagon))
        assertNotEquals(KapowEnter.dissolve(), KapowEnter.dissolve(seed = 5))
        assertNotEquals(KapowExit.dissolve(), KapowExit.dissolve(softness = 0.2f))
        assertNotEquals(KapowEnter.checker(), KapowEnter.checker(order = CheckerOrder.Spiral))
        assertNotEquals(
            KapowExit.checker(order = CheckerOrder.Random),
            KapowExit.checker(order = CheckerOrder.Random, seed = 9),
        )
        assertEquals(KapowEnter.checker(cellSize = 24.dp), KapowEnter.checker())
    }
}
