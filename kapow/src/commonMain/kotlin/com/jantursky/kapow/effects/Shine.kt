// The band profile (sin² falloff at 80 % strength), the ease-in-out sweep and the cooldown of half the
// duration are derived from Pow (https://github.com/EmergeTools/Pow), MIT, see NOTICE.
package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.ProgressRunner
import com.jantursky.kapow.internal.at
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration

/** Where the light band's gradient starts and ends at [progress], for a sweep in the direction of [angle] degrees. */
internal fun shineBand(
    size: Size,
    angle: Float,
    progress: Float,
    bandFraction: Float = BAND_FRACTION,
    lag: Float = 0f,
    extra: Float = 0f,
): Pair<Offset, Offset> {
    val radians = angle * PI.toFloat() / 180f
    val direction = Offset(cos(radians), sin(radians))
    // How long the element is when measured along the sweep direction.
    val length = abs(size.width * direction.x) + abs(size.height * direction.y)
    val band = length * bandFraction
    val travel = length / 2 + band / 2
    // A sweep with several bands travels [extra] further, so the last band leaves too; each trails by its [lag].
    val center =
        Offset(size.width / 2, size.height / 2) + direction * (-travel + (2 * travel + extra) * progress - lag)
    return (center - direction * (band / 2)) to (center + direction * (band / 2))
}

/**
 * The extra travel, and the lag of each band behind the first, of a sweep of [bands] bands that are [spacing]
 * band widths apart on an element [length] long along the sweep.
 */
internal fun shineLag(
    size: Size,
    angle: Float,
    bandFraction: Float,
    spacing: Float,
    index: Int,
): Float {
    val radians = angle * PI.toFloat() / 180f
    val length = abs(size.width * cos(radians)) + abs(size.height * sin(radians))
    return length * bandFraction * spacing * index
}

/**
 * The band's colors: transparent at both edges, strongest in the middle at [peak] of the color's alpha. A
 * [softness] below 1 reaches the peak sooner and holds it longer, so the edges are harder.
 */
internal fun shineStops(
    color: Color,
    softness: Float = 1f,
    peak: Float = 0.8f,
): Array<Pair<Float, Color>> {
    val steps = if (softness == 1f) BAND_STEPS else BAND_STEPS * 2
    return Array(steps + 1) { i ->
        val x = i.toFloat() / steps
        val s = sin(x * PI.toFloat())
        val profile = if (softness == 1f) s * s else (s * s / softness).coerceAtMost(1f)
        x to color.copy(alpha = color.alpha * profile * peak)
    }
}

/** Draws the band over the element's own pixels only. The caller keeps the layer offscreen while it draws. */
internal fun DrawScope.drawShine(
    stops: Array<Pair<Float, Color>>,
    angle: Float,
    progress: Float,
    alpha: Float = 1f,
    bandFraction: Float = BAND_FRACTION,
    bands: Int = 1,
    spacing: Float = 0f,
    blendMode: BlendMode = BlendMode.SrcAtop,
) {
    val extra = if (bands > 1) shineLag(size, angle, bandFraction, spacing, bands - 1) else 0f
    for (i in 0 until bands) {
        val lag = if (i == 0) 0f else shineLag(size, angle, bandFraction, spacing, i)
        val (start, end) = shineBand(size, angle, progress, bandFraction, lag, extra)
        drawRect(Brush.linearGradient(*stops, start = start, end = end), alpha = alpha, blendMode = blendMode)
    }
}

internal class ShineRunner(
    private val angle: Float,
    private val duration: Duration,
    color: Color,
    private val easing: Easing = FastOutSlowInEasing,
    private val bandFraction: Float = BAND_FRACTION,
    softness: Float = 1f,
    peak: Float = 0.8f,
    private val blendMode: BlendMode = BlendMode.SrcAtop,
    private val bands: Int = 1,
    private val spacing: Float = 1.5f,
    reverse: Boolean = false,
) : ChangeRunner() {
    private val sweepAngle = if (reverse) angle + 180f else angle
    private val stops = shineStops(color, softness, peak)
    private val sweep = Animatable(0f)
    private val flash = Animatable(0f)
    private var job: Job? = null

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        // SrcAtop needs the element's pixels in their own buffer; only pay for it while something is drawn.
        layer.compositingStrategy =
            if (sweep.isRunning || flash.value > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        if (sweep.isRunning) {
            val t = sweep.value
            drawShine(stops, sweepAngle, t, 1f - t.pow8(), bandFraction, bands, spacing, blendMode)
        }
        val f = flash.value
        if (f > 0f) drawRect(stops[stops.size / 2].second, alpha = f, blendMode = BlendMode.SrcAtop)
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        val millis = duration.at(context.speed).inWholeMilliseconds.toInt()
        job =
            context.scope.launch {
                if (context.isReducedMotion()) {
                    if (!context.showsReducedStandIn()) return@launch
                    flash.snapTo(0.5f)
                    flash.animateTo(0f, tween(millis / 2))
                } else {
                    sweep.snapTo(0f)
                    sweep.animateTo(1f, tween(millis, easing = easing))
                    sweep.snapTo(0f)
                }
            }
    }

    override fun stop() {
        job?.cancel()
    }
}

internal class ShineProgressRunner(
    private val angle: Float,
    color: Color,
) : ProgressRunner() {
    private val stops = shineStops(color)

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy = CompositingStrategy.Offscreen
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val t = progress().coerceIn(0f, 1f)
        if (t > 0f && t < 1f) drawShine(stops, angle, t)
    }
}

private fun Float.pow8(): Float {
    val sq = this * this
    val q = sq * sq
    return q * q
}

private const val BAND_FRACTION = 0.6f
private const val BAND_STEPS = 10
