package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The corners of a comic star burst with [spikes] points, inside a circle of radius 1 around (0, 0), as x0, y0,
 * x1, y1 and so on. Tips and notches alternate, starting with a tip straight up. Each corner is nudged by a seeded
 * jitter in radius and angle so the burst looks drawn by hand rather than regular.
 */
internal fun burstOutline(
    spikes: Int,
    seed: Int,
): FloatArray {
    val random = Random(seed)
    val corners = spikes * 2
    val step = 2 * PI.toFloat() / corners
    val points = FloatArray(corners * 2)
    for (k in 0 until corners) {
        val radius =
            if (k % 2 == 0) {
                TIP_MIN + (1f - TIP_MIN) * random.nextFloat()
            } else {
                NOTCH_MIN + NOTCH_JITTER * random.nextFloat()
            }
        val angle = -PI.toFloat() / 2 + k * step + (random.nextFloat() - 0.5f) * step * ANGLE_JITTER
        points[2 * k] = radius * cos(angle)
        points[2 * k + 1] = radius * sin(angle)
    }
    return points
}

/** How far the burst reaches from the element's center: past its corners by half its shorter side, at most [maxExtraPx]. */
internal fun burstRadius(
    size: Size,
    maxExtraPx: Float,
): Float = hypot(size.width, size.height) / 2 + min(min(size.width, size.height) / 2, maxExtraPx)

/** The burst's size at [progress], as a factor of its radius: it pops out past full size, then eases back a little. */
internal fun burstScale(
    progress: Float,
    peak: Float = PEAK,
    end: Float = END,
): Float {
    val p = progress.coerceIn(0f, 1f)
    if (p < POP) {
        val rest = 1f - p / POP
        return START + (peak - START) * (1f - rest * rest * rest)
    }
    return peak - (peak - end) * (p - POP) / (1f - POP)
}

/** The burst's peak size for an [overshoot] past full size, and the size it eases back to. */
internal fun burstPeak(overshoot: Float): Pair<Float, Float> =
    (PEAK + (overshoot - DEFAULT_BURST_OVERSHOOT)) to (END + (overshoot - DEFAULT_BURST_OVERSHOOT) / 3f)

internal const val DEFAULT_BURST_OVERSHOOT = 0.15f

/** The burst's opacity at [progress]: solid for the first half, then fading out. */
internal fun burstOpacity(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return if (p < 0.5f) 1f else 1f - (p - 0.5f) / 0.5f
}

private const val TIP_MIN = 0.82f
private const val NOTCH_MIN = 0.5f
private const val NOTCH_JITTER = 0.12f
private const val ANGLE_JITTER = 0.5f
private const val POP = 0.3f
private const val START = 0.3f
private const val PEAK = 1.15f
private const val END = 1.05f

/** A spiky comic burst behind the element, in two tones, that pops out, turns a little and fades. */
internal class BurstRunner(
    color: Color,
    private val spikes: Int,
    private val sizeScale: Float = 1f,
    coreColor: Color = Color.Unspecified,
    private val rotation: Float = 14f,
    overshoot: Float = DEFAULT_BURST_OVERSHOOT,
    private val innerRatio: Float = 0.72f,
    private val anchor: TransformOrigin = TransformOrigin.Center,
    private val inFront: Boolean = false,
    private val outline: Dp = 0.dp,
    private val outlineColor: Color = Color(0xFF1B1B1F),
    private val duration: Duration = 600.milliseconds,
    firstSeed: Int = 0,
) : ChangeRunner() {
    private val outer = color
    private val inner =
        if (coreColor.isSpecified) coreColor else lerp(color, Color.White, 0.55f).copy(alpha = color.alpha)
    private val peak = burstPeak(overshoot)
    private val outerPath = Path()
    private val innerPath = Path()
    private val progress = Animatable(0f)
    private val flash = Flash()
    private var job: Job? = null

    // Each burst gets the next seed: a different outline from the last, the same from run to run.
    private var seed = firstSeed

    // Only the reduced-motion brightening blends with the element's pixels. The burst itself reaches outside the
    // element, where a layer would cut it off.
    override val needsLayer: Boolean get() = flash.isVisible

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy = flash.compositingStrategy
    }

    override fun ContentDrawScope.drawEffect() {
        if (!inFront) drawBurst()
        drawContent()
        if (inFront) drawBurst()
        flash.draw(this)
    }

    private fun DrawScope.drawBurst() {
        if (!progress.isRunning) return
        val p = progress.value
        val radius = burstRadius(size, MAX_EXTRA.toPx()) * sizeScale * burstScale(p, peak.first, peak.second)
        val alpha = burstOpacity(p)
        translate(size.width * anchor.pivotFractionX, size.height * anchor.pivotFractionY) {
            rotate(rotation * p, pivot = Offset.Zero) {
                scale(radius, radius, pivot = Offset.Zero) {
                    drawPath(outerPath, outer, alpha)
                    scale(innerRatio, innerRatio, pivot = Offset.Zero) { drawPath(innerPath, inner, alpha) }
                    // The stroke is drawn inside the scaled space, so its width is divided by the scale.
                    if (outline > 0.dp && radius > 0f) {
                        drawPath(
                            outerPath,
                            outlineColor,
                            alpha,
                            Stroke(outline.toPx() / radius, join = StrokeJoin.Round),
                        )
                    }
                }
            }
        }
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        if (context.isReducedMotion()) {
            if (context.showsReducedStandIn()) job = context.scope.launch { flash.play(context) }
            return
        }
        outerPath.setPolygon(burstOutline(spikes, seed))
        innerPath.setPolygon(burstOutline(spikes, seed + INNER_SEED))
        seed++
        job =
            context.scope.launch {
                progress.snapTo(0f)
                progress.animateTo(
                    1f,
                    tween(duration.at(context.speed).inWholeMilliseconds.toInt(), easing = LinearEasing),
                )
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        const val INNER_SEED = 1000
        val MAX_EXTRA = 28.dp
    }
}

private fun Path.setPolygon(points: FloatArray) {
    reset()
    moveTo(points[0], points[1])
    for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1])
    close()
}
