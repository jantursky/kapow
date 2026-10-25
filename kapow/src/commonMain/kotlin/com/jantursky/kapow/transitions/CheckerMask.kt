package com.jantursky.kapow.transitions

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import com.jantursky.kapow.CheckerOrder
import com.jantursky.kapow.DEFAULT_CHECKER_SEED
import com.jantursky.kapow.TransitionKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max

/**
 * How far the checker cell at [column], [row] is open at [progress], from 0 to 1. Cells of one color, where
 * [column] + [row] is even, open over the first [CHECKER_WAVE] of the progress, the others over the last.
 */
internal fun checkerOpening(
    column: Int,
    row: Int,
    progress: Float,
): Float = checkerOpening(if ((column + row) % 2 == 0) 0f else 1f - CHECKER_WAVE, progress)

/** How far a cell that starts opening at [start] is open at [progress]. It takes [CHECKER_WAVE] to open. */
internal fun checkerOpening(
    start: Float,
    progress: Float,
): Float = ((progress - start) / CHECKER_WAVE).coerceIn(0f, 1f)

/**
 * When the cell at [column], [row] of a grid [columns] by [rows] starts to open, from 0 to 1 - [CHECKER_WAVE], in
 * the given [order]. [CheckerOrder.Together] opens the two colors of the board in two waves; the others spread the
 * starts over the whole transition, from one corner, at random (seeded by [seed]), or from the middle outward in a
 * turn.
 */
internal fun checkerStart(
    column: Int,
    row: Int,
    columns: Int,
    rows: Int,
    order: CheckerOrder,
    seed: Int,
): Float {
    val span = 1f - CHECKER_WAVE
    return when (order) {
        CheckerOrder.Together -> {
            if ((column + row) % 2 == 0) 0f else span
        }

        CheckerOrder.Diagonal -> {
            (column + row).toFloat() / max(1, columns + rows - 2) * span
        }

        CheckerOrder.Random -> {
            cellHash(column, row, seed) * span
        }

        CheckerOrder.Spiral -> {
            val dx = column - (columns - 1) / 2f
            val dy = row - (rows - 1) / 2f
            val ring = max(abs(dx), abs(dy))
            val turn = (atan2(dy, dx) + PI.toFloat()) / (2 * PI.toFloat())
            val outer = max((columns - 1) / 2f, (rows - 1) / 2f)
            ((ring + turn) / (outer + 1f)).coerceIn(0f, 1f) * span
        }
    }
}

/** The waves overlap a little, so the second starts before the first has finished. */
internal const val CHECKER_WAVE = 0.6f

/**
 * What is still hidden at [progress] of a checkerboard of about [cell] px squares: around each cell, the frame
 * outside the square that grows from its middle. The cells open in the given [order].
 */
internal fun checkerHidden(
    size: Size,
    cell: Float,
    progress: Float,
    order: CheckerOrder = CheckerOrder.Together,
    seed: Int = DEFAULT_CHECKER_SEED,
): Path {
    val grid = cellGrid(size, cell)
    return Path().apply {
        for (row in 0 until grid.height) {
            val top = cellEdge(row, grid.height, size.height)
            val bottom = cellEdge(row + 1, grid.height, size.height)
            for (column in 0 until grid.width) {
                val open =
                    checkerOpening(checkerStart(column, row, grid.width, grid.height, order, seed), progress)
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
        drawPath(
            checkerHidden(size, kind.cellSize.toPx(), progress, kind.order, kind.seed),
            Color.Black,
            blendMode = BlendMode.DstOut,
        )
    }
}
