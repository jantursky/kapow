package com.jantursky.kapow.transitions

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import com.jantursky.kapow.TransitionKind

/** Whether the cell at [column], [row] shows at [progress]. Each cell has its own fixed moment between 0 and 1. */
internal fun dissolveShows(
    column: Int,
    row: Int,
    progress: Float,
): Boolean = cellHash(column, row, DISSOLVE_SEED) < progress

/** The cells of about [cell] px still hidden at [progress], as one path. Hidden neighbours in a row are merged. */
internal fun dissolveHidden(
    size: Size,
    cell: Float,
    progress: Float,
): Path {
    val grid = cellGrid(size, cell)
    return Path().apply {
        for (row in 0 until grid.height) {
            val top = cellEdge(row, grid.height, size.height)
            val bottom = cellEdge(row + 1, grid.height, size.height)
            var start = -1
            for (column in 0..grid.width) {
                val hidden = column < grid.width && !dissolveShows(column, row, progress)
                if (hidden && start < 0) start = column
                if (!hidden && start >= 0) {
                    addRect(
                        Rect(
                            cellEdge(start, grid.width, size.width),
                            top,
                            cellEdge(column, grid.width, size.width),
                            bottom,
                        ),
                    )
                    start = -1
                }
            }
        }
    }
}

private const val DISSOLVE_SEED = 31

/** Shows the content cell by cell in a fixed random order; the exit hides them in the reverse order. */
internal class DissolveMask(
    private val kind: TransitionKind.Dissolve,
) : TransitionPart() {
    override fun ContentDrawScope.draw(progress: Float) {
        drawContent()
        drawPath(dissolveHidden(size, kind.cellSize.toPx(), progress), Color.Black, blendMode = BlendMode.DstOut)
    }
}
