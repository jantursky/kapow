package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import com.jantursky.kapow.transitions.farthestCorner
import com.jantursky.kapow.transitions.originPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** The ripple's radius at [progress]: quick at first, slowing down as it reaches [maxRadius]. */
internal fun rippleRadius(
    progress: Float,
    maxRadius: Float,
): Float {
    val rest = 1f - progress.coerceIn(0f, 1f)
    return maxRadius * (1f - rest * rest * rest)
}

/** The ripple's opacity at [progress]: full while it spreads, then fading out over the last 60 %. */
internal fun rippleOpacity(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return if (p < RIPPLE_HOLD) 1f else 1f - (p - RIPPLE_HOLD) / (1f - RIPPLE_HOLD)
}

private const val RIPPLE_HOLD = 0.4f

/**
 * How far ring [index] of [rings] has come at the whole ripple's [progress]. Each ring starts [spacing] of the
 * ripple later than the one before, and all of them finish together with the ripple.
 */
internal fun ringProgress(
    progress: Float,
    index: Int,
    rings: Int,
    spacing: Float,
): Float = ((progress - index * spacing) / (1f - (rings - 1) * spacing)).coerceIn(0f, 1f)

/** An ink circle that spreads from an origin over the element, clipped to its shape. */
internal class RippleRunner(
    private val color: Color,
    private val shape: Shape,
    private val origin: TransformOrigin,
    private val duration: Duration,
    private val rings: Int = 1,
    private val ringWidth: Dp = Dp.Unspecified,
    private val maxRadius: Dp = Dp.Unspecified,
    private val ringSpacing: Float = 0.2f,
    private val easing: Easing = LinearEasing,
) : ChangeRunner() {
    private val spread = Animatable(0f)
    private val tint = Animatable(0f)
    private var job: Job? = null

    // The clip only changes with the element's size, so it is built once per size rather than every frame.
    private val clip = Path()
    private var clipSize = Size.Unspecified
    private var clipDirection: LayoutDirection? = null

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val running = spread.isRunning
        val t = tint.value
        if (!running && t <= 0f) return
        clipPath(clipFor(this)) {
            if (running) {
                val point = originPoint(size, origin)
                val reach = if (maxRadius == Dp.Unspecified) farthestCorner(size, point) else maxRadius.toPx()
                val stroke = if (ringWidth == Dp.Unspecified) null else Stroke(ringWidth.toPx())
                for (i in 0 until rings) {
                    val p = ringProgress(spread.value, i, rings, ringSpacing)
                    if (i > 0 && (p <= 0f || p >= 1f)) continue
                    val radius = rippleRadius(p, reach)
                    if (stroke == null) {
                        drawCircle(color, radius, point, alpha = rippleOpacity(p))
                    } else {
                        drawCircle(color, radius, point, alpha = rippleOpacity(p), style = stroke)
                    }
                }
            }
            if (t > 0f) drawRect(color, alpha = t)
        }
    }

    private fun clipFor(scope: DrawScope): Path {
        if (scope.size != clipSize || scope.layoutDirection != clipDirection) {
            clip.reset()
            clip.addOutline(shape.createOutline(scope.size, scope.layoutDirection, scope))
            clipSize = scope.size
            clipDirection = scope.layoutDirection
        }
        return clip
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        val millis = duration.at(context.speed).inWholeMilliseconds.toInt()
        job =
            context.scope.launch {
                if (context.isReducedMotion()) {
                    if (!context.showsReducedStandIn()) return@launch
                    tint.snapTo(0.6f)
                    tint.animateTo(
                        0f,
                        tween(
                            300
                                .milliseconds
                                .at(context.speed)
                                .inWholeMilliseconds
                                .toInt(),
                        ),
                    )
                } else {
                    spread.snapTo(0f)
                    spread.animateTo(1f, tween(millis, easing = easing))
                }
            }
    }

    override fun stop() {
        job?.cancel()
    }
}
