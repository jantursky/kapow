package com.jantursky.kapow.transitions

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A fixed pseudo-random value in 0 until 1 for the cell at [x], [y]. The same arguments always give the same value
 * on every target, so a seeded pattern looks the same everywhere and needs no stored state.
 */
internal fun cellHash(
    x: Int,
    y: Int,
    seed: Int,
): Float {
    var h = x * 374761393 + y * 668265263 + seed * 1442695041
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFFFF) / 16777216f
}

/**
 * How many cells of about [cell] px cover [size] on each axis. Large elements get bigger cells, so a frame never
 * draws more than [MAX_CELLS].
 */
internal fun cellGrid(
    size: Size,
    cell: Float,
): IntSize {
    val area = size.width * size.height
    val side = if (area / (cell * cell) > MAX_CELLS) sqrt(area / MAX_CELLS) else cell
    return IntSize(max(1, ceil(size.width / side).toInt()), max(1, ceil(size.height / side).toInt()))
}

/**
 * Where line [index] of a grid of [count] cells across [extent] px falls, on a whole pixel so neighbouring cells
 * meet without a seam.
 */
internal fun cellEdge(
    index: Int,
    count: Int,
    extent: Float,
): Float = (index * extent / count).roundToInt().toFloat()

internal const val MAX_CELLS = 4096
