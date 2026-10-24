package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import kotlin.math.sqrt
import kotlin.time.Duration

/**
 * Splits the stretch from [from] to [to] px along a closed outline [length] px long into at most two plain spans,
 * wrapping past the start. Returns their ends as from, to pairs.
 */
internal fun wrappedSpans(
    from: Float,
    to: Float,
    length: Float,
): FloatArray {
    if (length <= 0f || to <= from) return FloatArray(0)
    if (to - from >= length) return floatArrayOf(0f, length)
    val start = ((from % length) + length) % length
    val end = start + (to - from)
    return if (end <= length) floatArrayOf(start, end) else floatArrayOf(start, length, 0f, end - length)
}

/** How bright the beam is at [position] along it, from 0 at the tail to 1 at the head. */
internal fun beamTailAlpha(position: Float): Float {
    val p = position.coerceIn(0f, 1f)
    return p * sqrt(p)
}

/** A short beam of light that travels around the outline of [shape], on a faint track. */
internal class BorderBeamRunner(
    private val color: Color,
    private val width: Dp,
    shape: Shape,
    private val length: Float,
    private val duration: Duration,
) : FadingLoopRunner() {
    private val outline = ShapeOutline(shape)
    private val piece = Path()

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val s = strength
        if (s <= 0f) return
        val stroke = width.toPx()
        outline.update(this, stroke / 2)
        val total = outline.length
        if (total <= 0f) return
        if (isReduced) {
            // Still: the whole outline, lit evenly.
            drawPath(outline.path, color, alpha = s * STILL_ALPHA, style = Stroke(stroke))
            return
        }
        drawPath(outline.path, color, alpha = s * TRACK_ALPHA, style = Stroke(stroke))
        val head = loopPhase(time, duration.inWholeMilliseconds / 1000.0) * total
        val beam = length * total
        // A soft glow around the front half of the beam, then the beam itself, fading toward its tail and
        // whitening toward its head.
        trace(head - beam / 2, head, total)
        drawPath(piece, color, alpha = s * HALO_ALPHA, style = Stroke(stroke * 4, cap = StrokeCap.Round))
        drawPath(piece, color, alpha = s * HALO_ALPHA, style = Stroke(stroke * 2.2f, cap = StrokeCap.Round))
        val core = Stroke(stroke, cap = StrokeCap.Butt)
        for (i in 0 until PIECES) {
            val position = (i + 1f) / PIECES
            trace(head - beam + beam * i / PIECES, head - beam + beam * position, total)
            val tint = lerp(color, Color.White, HEAD_WHITE * position * position * position)
            drawPath(piece, tint, alpha = beamTailAlpha(position) * s, style = core)
        }
    }

    /** Puts the stretch of the outline from [from] to [to] px into [piece]. */
    private fun trace(
        from: Float,
        to: Float,
        total: Float,
    ) {
        piece.reset()
        val spans = wrappedSpans(from, to, total)
        for (k in spans.indices step 2) outline.segment(spans[k], spans[k + 1], piece)
    }

    private companion object {
        const val PIECES = 24
        const val TRACK_ALPHA = 0.18f
        const val HALO_ALPHA = 0.14f
        const val HEAD_WHITE = 0.6f
        const val STILL_ALPHA = 0.7f
    }
}
