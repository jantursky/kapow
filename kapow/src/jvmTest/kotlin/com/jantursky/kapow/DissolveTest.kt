package com.jantursky.kapow

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DissolveTest {
    @Test
    fun entersCellByCell() = runTransitionTest { checkEnter(KapowEnter.dissolve(cellSize = 10.dp), midMillis = 200) }

    @Test
    fun exitsCellByCell() = runTransitionTest { checkExit(KapowExit.dissolve(cellSize = 10.dp), midMillis = 200) }

    @Test
    fun cellsAreWholeSquares() =
        runTransitionTest {
            set(enter = KapowEnter.dissolve(cellSize = 20.dp))
            show()
            advance(250)
            val image = capture()
            // Every 20 × 20 cell is either all shown or all hidden.
            for (row in 0 until 5) {
                for (column in 0 until 5) {
                    val x = 150 + column * 20
                    val y = 150 + row * 20
                    assertEquals(image.colorAt(x + 2, y + 2), image.colorAt(x + 17, y + 17), "cell $column, $row")
                }
            }
        }

    @Test
    fun theOrderIsTheSameEveryTime() {
        var first = 0f
        runTransitionTest {
            set(enter = KapowEnter.dissolve())
            show()
            advance(200)
            first = shown()
        }
        runTransitionTest {
            set(enter = KapowEnter.dissolve())
            show()
            advance(200)
            assertEquals(first, shown())
        }
    }

    @Test
    fun reducedMotionFades() = runTransitionTest { checkReducedFades(KapowEnter.dissolve(), KapowExit.dissolve()) }

    @Test
    fun framesDoNotRecompose() =
        runTransitionTest { checkFramesDoNotRecompose(KapowEnter.dissolve(), KapowExit.dissolve()) }

    @Test
    fun rejectsTinyCells() {
        assertFailsWith<IllegalArgumentException> { KapowEnter.dissolve(cellSize = 1.dp) }
        assertFailsWith<IllegalArgumentException> { KapowExit.dissolve(cellSize = 1.dp) }
        assertTrue(KapowEnter.dissolve() == KapowEnter.dissolve())
    }

    @Test
    fun keyFrame() = runTransitionTest { keyFrame("dissolve_enter_200", enter = KapowEnter.dissolve(), millis = 200) }
}
