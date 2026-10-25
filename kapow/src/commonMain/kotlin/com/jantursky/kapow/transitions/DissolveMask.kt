package com.jantursky.kapow.transitions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import com.jantursky.kapow.CellShape
import com.jantursky.kapow.DEFAULT_DISSOLVE_SEED
import com.jantursky.kapow.TransitionKind
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Whether the cell at [column], [row] shows at [progress]. Each cell has its own fixed moment between 0 and 1. */
internal fun dissolveShows(
    column: Int,
    row: Int,
    progress: Float,
    seed: Int = DEFAULT_DISSOLVE_SEED,
): Boolean = cellHash(column, row, seed) < progress

/**
 * How far a cell whose moment is [moment] (0 to 1) has grown at [progress], from 0 to 1. With a [softness] of 0 it
 * pops to full size when the progress passes its moment. Otherwise it grows over [softness] of the transition,
 * starting early enough that every cell is whole at progress 1.
 */
internal fun cellGrowth(
    moment: Float,
    progress: Float,
    softness: Float,
): Float {
    if (softness <= 0f) return if (moment < progress) 1f else 0f
    return ((progress - moment * (1f - softness)) / softness).coerceIn(0f, 1f)
}

/** The cells of about [cell] px still hidden at [progress], as one path. Hidden neighbours in a row are merged. */
internal fun dissolveHidden(
    size: Size,
    cell: Float,
    progress: Float,
    seed: Int = DEFAULT_DISSOLVE_SEED,
): Path {
    val grid = cellGrid(size, cell)
    return Path().apply {
        for (row in 0 until grid.height) {
            val top = cellEdge(row, grid.height, size.height)
            val bottom = cellEdge(row + 1, grid.height, size.height)
            var start = -1
            for (column in 0..grid.width) {
                val hidden = column < grid.width && !dissolveShows(column, row, progress, seed)
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

/**
 * What shows at [progress] of cells of about [cell] px in the given [shape], as one path: each cell is its shape
 * scaled by how far it has grown ([cellGrowth] with [softness]). At full growth squares and hexagons tile the
 * content exactly, and circles are as wide as their cell's diagonal, so they overlap and cover it.
 */
internal fun dissolveShown(
    size: Size,
    cell: Float,
    progress: Float,
    shape: CellShape,
    softness: Float,
    seed: Int,
): Path =
    Path().apply {
        if (shape == CellShape.Hexagon) {
            val side = max(cell, sqrt(size.width * size.height / MAX_CELLS))
            val radius = side * HEX_SCALE
            val dx = 1.5f * radius
            val dy = sqrt(3f) * radius
            val columns = ceil(size.width / dx).toInt() + 1
            val rows = ceil(size.height / dy).toInt() + 1
            for (column in -1..columns) {
                for (row in -1..rows) {
                    val g = cellGrowth(cellHash(column, row, seed), progress, softness)
                    if (g <= 0f) continue
                    val cx = column * dx
                    val cy = row * dy + if (column and 1 == 1) dy / 2 else 0f
                    addHexagon(Offset(cx, cy), radius * g)
                }
            }
            return@apply
        }
        val grid = cellGrid(size, cell)
        for (row in 0 until grid.height) {
            val top = cellEdge(row, grid.height, size.height)
            val bottom = cellEdge(row + 1, grid.height, size.height)
            for (column in 0 until grid.width) {
                val g = cellGrowth(cellHash(column, row, seed), progress, softness)
                if (g <= 0f) continue
                val left = cellEdge(column, grid.width, size.width)
                val right = cellEdge(column + 1, grid.width, size.width)
                val middle = Offset((left + right) / 2, (top + bottom) / 2)
                if (shape == CellShape.Circle) {
                    addOval(Rect(middle, hypot(right - left, bottom - top) / 2 * g))
                } else {
                    addRect(
                        Rect(
                            middle.x - (right - left) * g / 2,
                            middle.y - (bottom - top) * g / 2,
                            middle.x + (right - left) * g / 2,
                            middle.y + (bottom - top) * g / 2,
                        ),
                    )
                }
            }
        }
    }

/** A flat-topped hexagon around [center] whose corners are [radius] px away. */
private fun Path.addHexagon(
    center: Offset,
    radius: Float,
) {
    for (i in 0 until 6) {
        val angle = PI.toFloat() / 3f * i
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** How a hexagon's corner radius relates to the cell size asked for, so a hexagon is about as big as a cell. */
private const val HEX_SCALE = 0.58f

/** Shows the content cell by cell in a fixed random order; the exit hides them in the reverse order. */
internal class DissolveMask(
    private val kind: TransitionKind.Dissolve,
) : TransitionPart() {
    override fun ContentDrawScope.draw(progress: Float) {
        val cell = kind.cellSize.toPx()
        if (kind.cellShape == CellShape.Square && kind.softness == 0f) {
            drawContent()
            drawPath(dissolveHidden(size, cell, progress, kind.seed), Color.Black, blendMode = BlendMode.DstOut)
            return
        }
        val shown = dissolveShown(size, cell, progress, kind.cellShape, kind.softness, kind.seed)
        clipPath(shown) { this@draw.drawContent() }
    }
}
