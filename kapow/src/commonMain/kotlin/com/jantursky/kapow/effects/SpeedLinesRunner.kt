package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

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
): FloatArray {
    val random = Random(seed)
    val step = 2 * PI.toFloat() / count
    val start = random.nextFloat() * step
    val lines = FloatArray(count * 3)
    for (i in 0 until count) {
        lines[3 * i] = start + i * step + (random.nextFloat() - 0.5f) * step * 0.6f
        lines[3 * i + 1] = 0.55f + 0.45f * random.nextFloat()
        lines[3 * i + 2] = 0.6f + 0.4f * random.nextFloat()
    }
    return lines
}

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

/** Comic action lines that fly out from the element's edges and vanish. */
internal class SpeedLinesRunner(
    private val color: Color,
    private val count: Int,
) : ChangeRunner() {
    private val progress = Animatable(0f)
    private val flash = Flash()
    private var lines = FloatArray(0)
    private var job: Job? = null
    private var seed = 0

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
        val reach = speedLineReach(size, MIN_REACH.toPx(), MAX_REACH.toPx())
        val gap = GAP.toPx()
        val width = WIDTH.toPx()
        val center = Offset(size.width / 2, size.height / 2)
        for (i in 0 until lines.size / 3) {
            val angle = lines[3 * i]
            val length = reach * lines[3 * i + 1]
            val from = edgeDistance(size, angle) + gap
            val direction = Offset(cos(angle), sin(angle))
            drawLine(
                color,
                center + direction * (from + length * inner),
                center + direction * (from + length * outer),
                strokeWidth = width * lines[3 * i + 2],
                cap = StrokeCap.Round,
            )
        }
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        if (context.isReducedMotion()) {
            job = context.scope.launch { flash.play() }
            return
        }
        lines = speedLines(count, seed++)
        job =
            context.scope.launch {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(MILLIS, easing = LinearEasing))
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        const val MILLIS = 500
        val GAP = 6.dp
        val WIDTH = 3.dp
        val MIN_REACH = 16.dp
        val MAX_REACH = 40.dp
    }
}
