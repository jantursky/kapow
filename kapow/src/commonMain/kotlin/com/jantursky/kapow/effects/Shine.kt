// The band profile (sin² falloff at 80 % strength), the ease-in-out sweep and the cooldown of half the
// duration are derived from Pow (https://github.com/EmergeTools/Pow), MIT, see NOTICE.
package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.ProgressRunner
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
): Pair<Offset, Offset> {
    val radians = angle * PI.toFloat() / 180f
    val direction = Offset(cos(radians), sin(radians))
    // How long the element is when measured along the sweep direction.
    val length = abs(size.width * direction.x) + abs(size.height * direction.y)
    val band = length * BandFraction
    val travel = length / 2 + band / 2
    val center = Offset(size.width / 2, size.height / 2) + direction * (-travel + 2 * travel * progress)
    return (center - direction * (band / 2)) to (center + direction * (band / 2))
}

/** The band's colors: transparent at both edges, strongest in the middle. */
internal fun shineStops(color: Color): Array<Pair<Float, Color>> =
    Array(BandSteps + 1) { i ->
        val x = i.toFloat() / BandSteps
        val s = sin(x * PI.toFloat())
        x to color.copy(alpha = color.alpha * s * s * 0.8f)
    }

/** Draws the band over the element's own pixels only. The caller keeps the layer offscreen while it draws. */
internal fun DrawScope.drawShine(
    stops: Array<Pair<Float, Color>>,
    angle: Float,
    progress: Float,
    alpha: Float = 1f,
) {
    val (start, end) = shineBand(size, angle, progress)
    drawRect(Brush.linearGradient(*stops, start = start, end = end), alpha = alpha, blendMode = BlendMode.SrcAtop)
}

internal class ShineRunner(
    private val angle: Float,
    private val duration: Duration,
    color: Color,
) : ChangeRunner() {
    private val stops = shineStops(color)
    private val sweep = Animatable(0f)
    private val flash = Animatable(0f)
    private var job: Job? = null

    override val needsLayer: Boolean get() = true

    override fun GraphicsLayerScope.applyLayer() {
        // SrcAtop needs the element's pixels in their own buffer; only pay for it while something is drawn.
        compositingStrategy =
            if (sweep.isRunning || flash.value > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        if (sweep.isRunning) {
            val t = sweep.value
            drawShine(stops, angle, t, alpha = 1f - t.pow8())
        }
        val f = flash.value
        if (f > 0f) drawRect(stops[BandSteps / 2].second, alpha = f, blendMode = BlendMode.SrcAtop)
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        val millis = duration.inWholeMilliseconds.toInt()
        job =
            context.scope.launch {
                if (context.isReducedMotion()) {
                    flash.snapTo(0.5f)
                    flash.animateTo(0f, tween(millis / 2))
                } else {
                    sweep.snapTo(0f)
                    sweep.animateTo(1f, tween(millis, easing = FastOutSlowInEasing))
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

    override fun GraphicsLayerScope.applyLayer() {
        compositingStrategy = CompositingStrategy.Offscreen
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

private const val BandFraction = 0.6f
private const val BandSteps = 10
