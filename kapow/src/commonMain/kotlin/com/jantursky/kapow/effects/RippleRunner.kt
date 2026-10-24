package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.transitions.farthestCorner
import com.jantursky.kapow.transitions.originPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration

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

/** An ink circle that spreads from an origin over the element, clipped to its shape. */
internal class RippleRunner(
    private val color: Color,
    private val shape: Shape,
    private val origin: TransformOrigin,
    private val duration: Duration,
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
                val p = spread.value
                val point = originPoint(size, origin)
                val radius = rippleRadius(p, farthestCorner(size, point))
                drawCircle(color, radius, point, alpha = rippleOpacity(p))
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
        val millis = duration.inWholeMilliseconds.toInt()
        job =
            context.scope.launch {
                if (context.isReducedMotion()) {
                    tint.snapTo(0.6f)
                    tint.animateTo(0f, tween(300))
                } else {
                    spread.snapTo(0f)
                    spread.animateTo(1f, tween(millis, easing = LinearEasing))
                }
            }
    }

    override fun stop() {
        job?.cancel()
    }
}
