package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.StrikeDirection
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import com.jantursky.kapow.internal.awaitFrameTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Where the strike line starts and ends when it has drawn [reach] (0 to 1) of the way across: through the middle
 * of [size] (or at [y] when given), from the start edge in the reading direction.
 */
internal fun strikeLine(
    size: Size,
    reach: Float,
    layoutDirection: LayoutDirection,
    y: Float = size.height / 2,
): Pair<Offset, Offset> {
    val r = reach.coerceIn(0f, 1f)
    return if (layoutDirection == LayoutDirection.Ltr) {
        Offset(0f, y) to Offset(size.width * r, y)
    } else {
        Offset(size.width, y) to Offset(size.width * (1f - r), y)
    }
}

/**
 * The line of a strike in [direction] when it has drawn [reach] of the way, as a start and a moving end.
 * [offset] moves it across the element, as a fraction of the element's size from the middle, and [shift] by that
 * many more pixels.
 */
internal fun strikeSegment(
    size: Size,
    reach: Float,
    direction: StrikeDirection,
    layoutDirection: LayoutDirection,
    offset: Float = 0f,
    shift: Float = 0f,
): Pair<Offset, Offset> {
    val r = reach.coerceIn(0f, 1f)
    val rtl = layoutDirection == LayoutDirection.Rtl
    return when (direction) {
        StrikeDirection.Reading -> {
            strikeLine(size, r, layoutDirection, size.height / 2 + offset * size.height + shift)
        }

        StrikeDirection.Reverse -> {
            val flipped = if (rtl) LayoutDirection.Ltr else LayoutDirection.Rtl
            strikeLine(size, r, flipped, size.height / 2 + offset * size.height + shift)
        }

        StrikeDirection.Vertical -> {
            val x = size.width / 2 + offset * size.width + shift
            Offset(x, 0f) to Offset(x, size.height * r)
        }

        StrikeDirection.Diagonal -> {
            val y = offset * size.height + shift
            val from = Offset(if (rtl) size.width else 0f, y)
            val to = Offset(if (rtl) 0f else size.width, size.height + y)
            from to (from + (to - from) * r)
        }
    }
}

/**
 * A wavy line from [from] to [to]: the straight line with a sine of [amplitude] and [length] (both in pixels)
 * added across it.
 */
internal fun wavyPath(
    from: Offset,
    to: Offset,
    amplitude: Float,
    length: Float,
): Path {
    val path = Path()
    val along = to - from
    val distance = along.getDistance()
    path.moveTo(from.x, from.y)
    if (distance <= 0f) return path
    val normal = Offset(-along.y / distance, along.x / distance)
    val steps = ceil(distance / WAVE_STEP).toInt().coerceAtLeast(1)
    for (i in 1..steps) {
        val d = distance * i / steps
        val bend = amplitude * sin(2f * PI.toFloat() * d / length)
        val point = from + along * (i.toFloat() / steps) + normal * bend
        path.lineTo(point.x, point.y)
    }
    return path
}

private const val WAVE_STEP = 2f

/** A line that draws across the element, holds for [hold], then fades out. */
internal class StrikeRunner(
    private val color: Color,
    private val width: Dp,
    private val hold: Duration,
    private val direction: StrikeDirection = StrikeDirection.Reading,
    private val drawDuration: Duration = 260.milliseconds,
    private val easing: Easing = FastOutSlowInEasing,
    private val offset: Float = 0f,
    private val cap: StrokeCap = StrokeCap.Butt,
    private val waveAmplitude: Dp = 0.dp,
    private val waveLength: Dp = 12.dp,
    private val lines: Int = 1,
    private val lineSpacing: Dp = 6.dp,
    private val brush: Brush? = null,
) : ChangeRunner() {
    private val reach = Animatable(0f)
    private val alpha = Animatable(0f)
    private var job: Job? = null

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val a = alpha.value
        val r = reach.value
        if (a <= 0f || r <= 0f) return
        val stroke = width.toPx()
        for (i in 0 until lines) {
            val shift = (i - (lines - 1) / 2f) * lineSpacing.toPx()
            drawStrike(strikeSegment(size, r, direction, layoutDirection, offset, shift), r, stroke, a)
        }
    }

    private fun DrawScope.drawStrike(
        line: Pair<Offset, Offset>,
        r: Float,
        stroke: Float,
        a: Float,
    ) {
        val (start, end) = line
        val brush = brush
        if (waveAmplitude > 0.dp) {
            val path = wavyPath(start, end, waveAmplitude.toPx(), waveLength.toPx())
            val style = Stroke(stroke, cap = cap)
            if (brush == null) drawPath(path, color, a, style) else drawPath(path, brush, a, style)
            return
        }
        // Butt ends inside the element, a round cap on the moving end.
        if (brush ==
            null
        ) {
            drawLine(color, start, end, stroke, cap, alpha = a)
        } else {
            drawLine(brush, start, end, stroke, cap, alpha = a)
        }
        if (r < 1f && cap == StrokeCap.Butt) {
            if (brush ==
                null
            ) {
                drawCircle(color, stroke / 2, end, alpha = a)
            } else {
                drawCircle(brush, stroke / 2, end, alpha = a)
            }
        }
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        val speed = context.speed
        job =
            context.scope.launch {
                alpha.snapTo(1f)
                if (context.isReducedMotion()) {
                    // The line appears whole instead of drawing across, in every reduced-motion mode.
                    reach.snapTo(1f)
                } else {
                    reach.snapTo(0f)
                    val millis = drawDuration.at(speed).inWholeMilliseconds.toInt()
                    reach.animateTo(1f, tween(millis, easing = easing))
                }
                awaitFrameTime(hold.at(speed))
                alpha.animateTo(0f, tween(FADE.at(speed).inWholeMilliseconds.toInt()))
                reach.snapTo(0f)
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        val FADE = 300.milliseconds
    }
}
