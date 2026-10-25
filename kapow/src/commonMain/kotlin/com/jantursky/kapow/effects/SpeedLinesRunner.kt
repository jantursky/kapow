package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.SpeedLineDirections
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** The distance from the center of [size] to its edge, in the direction of [angle] radians. */
internal fun edgeDistance(
    size: Size,
    angle: Float,
): Float {
    val x = abs(cos(angle))
    val y = abs(sin(angle))
    val toSide = if (x > 1e-6f) size.width / 2 / x else Float.MAX_VALUE
    val toTopOrBottom = if (y > 1e-6f) size.height / 2 / y else Float.MAX_VALUE
    return min(toSide, toTopOrBottom)
}

/**
 * [count] action lines around a full turn, as angle (radians), length and width, the last two as fractions of the
 * longest and widest line. The lines are spread evenly, each nudged by a seeded jitter so the ring looks drawn.
 */
internal fun speedLines(
    count: Int,
    seed: Int,
    directions: SpeedLineDirections = SpeedLineDirections.All,
): FloatArray {
    val random = Random(seed)
    val step = 2 * PI.toFloat() / count
    val start = random.nextFloat() * step
    val lines = FloatArray(count * 3)
    for (i in 0 until count) {
        lines[3 * i] = directions.fit(start + i * step + (random.nextFloat() - 0.5f) * step * 0.6f)
        lines[3 * i + 1] = 0.55f + 0.45f * random.nextFloat()
        lines[3 * i + 2] = 0.6f + 0.4f * random.nextFloat()
    }
    return lines
}

/**
 * Pulls an angle around the full turn, in radians, into the directions a speed-lines effect allows. Angles are
 * spread over the whole turn first, so each direction keeps its share of the lines.
 */
internal fun SpeedLineDirections.fit(angle: Float): Float {
    val turn = 2 * PI.toFloat()
    val fraction = ((angle % turn) + turn) % turn / turn
    val half = PI.toFloat()

    // Within a cone, 0..1 maps to 70 degrees around the cone's middle.
    fun cone(
        within: Float,
        middle: Float,
    ) = middle + (within - 0.5f) * CONE
    return when (this) {
        SpeedLineDirections.All -> angle
        SpeedLineDirections.Horizontal -> cone(fraction * 2 % 1f, if (fraction < 0.5f) 0f else half)
        SpeedLineDirections.Vertical -> cone(fraction * 2 % 1f, if (fraction < 0.5f) half / 2 else half * 1.5f)
        SpeedLineDirections.Arc -> -half + fraction * half
    }
}

private const val CONE = 70f * PI.toFloat() / 180f

/**
 * Where a line's inner and outer ends are at [progress], as fractions of its length: the outer end shoots out
 * first and the inner end follows, so the line flies off and is gone at the end.
 */
internal fun speedLineSpan(progress: Float): Pair<Float, Float> {
    val p = progress.coerceIn(0f, 1f)
    val lead = (p / LEAD).coerceAtMost(1f)
    val outer = 1f - (1f - lead) * (1f - lead)
    val inner = p * p
    return inner to outer
}

private const val LEAD = 0.55f

/** How long the longest line is: half the element's shorter side, kept between [minPx] and [maxPx]. */
internal fun speedLineReach(
    size: Size,
    minPx: Float,
    maxPx: Float,
): Float = (min(size.width, size.height) / 2).coerceIn(minPx, maxPx)

/**
 * A line from [tail] to [head] that narrows from [width] at the tail by [taper] (0 to 1) towards the head, where
 * 1 ends in a point.
 */
internal fun taperedLine(
    tail: Offset,
    head: Offset,
    width: Float,
    taper: Float,
): Path {
    val along = head - tail
    val length = along.getDistance()
    val normal = if (length > 0f) Offset(-along.y / length, along.x / length) else Offset.Zero
    val half = width / 2
    val end = half * (1f - taper)
    return Path().apply {
        moveTo(tail.x + normal.x * half, tail.y + normal.y * half)
        lineTo(head.x + normal.x * end, head.y + normal.y * end)
        lineTo(head.x - normal.x * end, head.y - normal.y * end)
        lineTo(tail.x - normal.x * half, tail.y - normal.y * half)
        close()
    }
}

/** Comic action lines that fly out from the element's edges and vanish. */
internal class SpeedLinesRunner(
    private val color: Color,
    private val count: Int,
    private val length: Dp = Dp.Unspecified,
    private val thickness: Dp = 3.dp,
    private val innerGap: Dp = 6.dp,
    private val taper: Float = 0f,
    private val directions: SpeedLineDirections = SpeedLineDirections.All,
    private val duration: Duration = 500.milliseconds,
    private val easing: Easing = LinearEasing,
    firstSeed: Int = 0,
) : ChangeRunner() {
    private val progress = Animatable(0f)
    private val flash = Flash()
    private var lines = FloatArray(0)
    private var job: Job? = null
    private var seed = firstSeed

    // Only the reduced-motion brightening needs the element's pixels; the lines are drawn outside the element.
    override val needsLayer: Boolean get() = flash.isVisible

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy = flash.compositingStrategy
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        flash.draw(this)
        if (!progress.isRunning) return
        val (inner, outer) = speedLineSpan(progress.value)
        val reach =
            if (length ==
                Dp.Unspecified
            ) {
                speedLineReach(size, MIN_REACH.toPx(), MAX_REACH.toPx())
            } else {
                length.toPx()
            }
        val gap = innerGap.toPx()
        val width = thickness.toPx()
        val center = Offset(size.width / 2, size.height / 2)
        for (i in 0 until lines.size / 3) {
            val angle = lines[3 * i]
            val length = reach * lines[3 * i + 1]
            val from = edgeDistance(size, angle) + gap
            val direction = Offset(cos(angle), sin(angle))
            val tail = center + direction * (from + length * inner)
            val head = center + direction * (from + length * outer)
            if (taper > 0f) {
                drawPath(taperedLine(tail, head, width * lines[3 * i + 2], taper), color)
            } else {
                drawLine(color, tail, head, strokeWidth = width * lines[3 * i + 2], cap = StrokeCap.Round)
            }
        }
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        if (context.isReducedMotion()) {
            if (context.showsReducedStandIn()) job = context.scope.launch { flash.play(context) }
            return
        }
        lines = speedLines(count, seed++, directions)
        val millis = duration.at(context.speed).inWholeMilliseconds.toInt()
        job =
            context.scope.launch {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(millis, easing = easing))
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        val MIN_REACH = 16.dp
        val MAX_REACH = 40.dp
    }
}
