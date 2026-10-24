package com.jantursky.kapow.transitions

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import com.jantursky.kapow.TransitionKind

/**
 * How far the checker cell at [column], [row] is open at [progress], from 0 to 1. Cells of one color, where
 * [column] + [row] is even, open over the first [CHECKER_WAVE] of the progress, the others over the last.
 */
internal fun checkerOpening(
    column: Int,
    row: Int,
    progress: Float,
): Float {
    val start = if ((column + row) % 2 == 0) 0f else 1f - CHECKER_WAVE
    return ((progress - start) / CHECKER_WAVE).coerceIn(0f, 1f)
}

/** The waves overlap a little, so the second starts before the first has finished. */
internal const val CHECKER_WAVE = 0.6f

/**
 * What is still hidden at [progress] of a checkerboard of about [cell] px squares: around each cell, the frame
 * outside the square that grows from its middle.
 */
internal fun checkerHidden(
    size: Size,
    cell: Float,
    progress: Float,
): Path {
    val grid = cellGrid(size, cell)
    return Path().apply {
        for (row in 0 until grid.height) {
            val top = cellEdge(row, grid.height, size.height)
            val bottom = cellEdge(row + 1, grid.height, size.height)
            for (column in 0 until grid.width) {
                val open = checkerOpening(column, row, progress)
                if (open >= 1f) continue
                val left = cellEdge(column, grid.width, size.width)
                val right = cellEdge(column + 1, grid.width, size.width)
                if (open <= 0f) {
                    addRect(Rect(left, top, right, bottom))
                    continue
                }
                val cx = (left + right) / 2
                val cy = (top + bottom) / 2
                val hw = (right - left) * open / 2
                val hh = (bottom - top) * open / 2
                addRect(Rect(left, top, right, cy - hh))
                addRect(Rect(left, cy + hh, right, bottom))
                addRect(Rect(left, cy - hh, cx - hw, cy + hh))
                addRect(Rect(cx + hw, cy - hh, right, cy + hh))
            }
        }
    }
}

/** Opens a checkerboard of squares in two waves, one color after the other; the exit closes them in reverse. */
internal class CheckerMask(
    private val kind: TransitionKind.Checker,
) : TransitionPart() {
    override fun ContentDrawScope.draw(progress: Float) {
        drawContent()
        drawPath(checkerHidden(size, kind.cellSize.toPx(), progress), Color.Black, blendMode = BlendMode.DstOut)
    }
}
