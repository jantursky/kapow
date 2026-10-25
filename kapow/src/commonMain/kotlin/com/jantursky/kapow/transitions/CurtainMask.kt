package com.jantursky.kapow.transitions

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.BlindsOrientation
import com.jantursky.kapow.TransitionKind
import kotlin.math.PI
import kotlin.math.sin

/**
 * How far each half of the curtain has moved off the middle of an [extent] px wide opening at [progress]. It goes
 * [fold] past the edge, so the folds in the curtain's hem clear the content too.
 */
internal fun curtainOpening(
    extent: Float,
    fold: Float,
    progress: Float,
): Float = curtainHalfOpening(extent / 2, fold, progress)

/** How far a curtain half has moved at [progress] when it has [reach] px to cover from the seam to its edge. */
internal fun curtainHalfOpening(
    reach: Float,
    fold: Float,
    progress: Float,
): Float = (reach + fold) * progress.coerceIn(0f, 1f)

/** How far the hem bulges at [along] px down the seam: folds [fold] px deep and [wavelength] px apart. */
internal fun curtainFold(
    along: Float,
    fold: Float,
    wavelength: Float,
): Float = fold * sin(2 * PI.toFloat() * along / wavelength)

/**
 * Two halves that part from the middle of the content. With a vertical seam they slide to the left and right, with
 * a horizontal one up and down. Their hems carry soft folds that run in step, so the opening is an even wavy band.
 */
internal class CurtainMask(
    private val kind: TransitionKind.Curtain,
) : TransitionPart() {
    override fun ContentDrawScope.draw(progress: Float) {
        drawContent()
        val vertical = kind.orientation == BlindsOrientation.Vertical
        val extent = if (vertical) size.width else size.height
        val length = if (vertical) size.height else size.width
        val fold = 3.dp.toPx()
        val wavelength = 28.dp.toPx()
        // The seam sits at [split] of the extent; each half moves just far enough to clear its own side.
        val middle = if (kind.split == 0.5f) extent / 2 else extent * kind.split
        val step = 4.dp.toPx()
        val halves = Path()
        for (side in listOf(-1f, 1f)) {
            val edge = if (side < 0) 0f else extent
            val open = curtainHalfOpening(if (side < 0) middle else extent - middle, fold, progress)
            point(halves, vertical, edge, 0f, move = true)
            var along = 0f
            while (true) {
                point(halves, vertical, middle + side * open + curtainFold(along, fold, wavelength), along)
                if (along >= length) break
                along = minOf(along + step, length)
            }
            point(halves, vertical, edge, length)
            halves.close()
        }
        drawPath(halves, Color.Black, blendMode = BlendMode.DstOut)
    }

    /** Adds a point [across] the seam and [along] it, in the frame of the orientation. */
    private fun point(
        path: Path,
        vertical: Boolean,
        across: Float,
        along: Float,
        move: Boolean = false,
    ) {
        val x = if (vertical) across else along
        val y = if (vertical) along else across
        if (move) path.moveTo(x, y) else path.lineTo(x, y)
    }
}
