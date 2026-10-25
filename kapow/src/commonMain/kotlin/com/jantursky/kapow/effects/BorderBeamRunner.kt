package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import kotlin.math.pow
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

/**
 * How bright the beam is at [position] along it when its tail fades with [exponent]: 1.5 is the default look,
 * higher values keep the tail dark for longer and 0 lights the whole beam evenly.
 */
internal fun beamTailAlpha(
    position: Float,
    exponent: Float,
): Float = if (exponent == 1.5f) beamTailAlpha(position) else position.coerceIn(0f, 1f).pow(exponent)

/**
 * Where the head of beam [index] of [beams] is on an outline [total] px long, [phase] (0 to 1) of the way round:
 * the beams are spaced evenly and go round [clockwise] or against it.
 */
internal fun beamHead(
    phase: Float,
    index: Int,
    beams: Int,
    total: Float,
    clockwise: Boolean,
): Float {
    val turn = if (clockwise) phase else 1f - phase
    return (((turn + index.toFloat() / beams) % 1f) + 1f) % 1f * total
}

/**
 * A short beam of light that travels around the outline of [shape], on a faint track. It is [beams] beams going
 * [clockwise] or not, stroked with [brush] when set (instead of [color]), with a halo of [glow] times the usual
 * strength and a tail that fades with [tailFade].
 */
internal class BorderBeamRunner(
    private val color: Color,
    private val width: Dp,
    shape: Shape,
    private val length: Float,
    private val duration: Duration,
    private val brush: Brush? = null,
    private val trackAlpha: Float = TRACK_ALPHA,
    private val beams: Int = 1,
    private val clockwise: Boolean = true,
    private val glow: Float = 1f,
    private val tailFade: Float = 1.5f,
) : FadingLoopRunner() {
    private val outline = ShapeOutline(shape)
    private val piece = Path()

    private fun DrawScope.stroke(
        path: Path,
        tint: Color,
        alpha: Float,
        style: Stroke,
    ) {
        if (brush !=
            null
        ) {
            drawPath(path, brush, alpha = alpha, style = style)
        } else {
            drawPath(path, tint, alpha = alpha, style = style)
        }
    }

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
            stroke(outline.path, color, s * STILL_ALPHA, Stroke(stroke))
            return
        }
        stroke(outline.path, color, s * trackAlpha, Stroke(stroke))
        val phase = loopPhase(time, duration.inWholeMilliseconds / 1000.0)
        val beam = length * total
        for (b in 0 until beams) {
            drawBeam(beamHead(phase, b, beams, total, clockwise), beam, total, s, stroke)
        }
    }

    private fun DrawScope.drawBeam(
        head: Float,
        beam: Float,
        total: Float,
        s: Float,
        stroke: Float,
    ) {
        // The beam from its tail (0) to its head ([beam]) as a stretch of the outline.
        fun span(
            from: Float,
            to: Float,
        ) = if (clockwise) {
            trace(head - beam + from, head - beam + to, total)
        } else {
            trace(
                head + beam - to,
                head + beam - from,
                total,
            )
        }
        // A soft glow around the front half of the beam, then the beam itself, fading toward its tail and
        // whitening toward its head.
        if (glow > 0f) {
            if (clockwise) trace(head - beam / 2, head, total) else trace(head, head + beam / 2, total)
            stroke(piece, color, s * HALO_ALPHA * glow, Stroke(stroke * 4, cap = StrokeCap.Round))
            stroke(piece, color, s * HALO_ALPHA * glow, Stroke(stroke * 2.2f, cap = StrokeCap.Round))
        }
        val core = Stroke(stroke, cap = StrokeCap.Butt)
        for (i in 0 until PIECES) {
            val position = (i + 1f) / PIECES
            span(beam * i / PIECES, beam * position)
            val tint =
                if (brush !=
                    null
                ) {
                    color
                } else {
                    lerp(color, Color.White, HEAD_WHITE * position * position * position)
                }
            stroke(piece, tint, beamTailAlpha(position, tailFade) * s, core)
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
