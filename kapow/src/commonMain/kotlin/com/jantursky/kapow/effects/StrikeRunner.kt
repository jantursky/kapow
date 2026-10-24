package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.awaitFrameTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Where the strike line starts and ends when it has drawn [reach] (0 to 1) of the way across: through the middle
 * of [size], from the start edge in the reading direction.
 */
internal fun strikeLine(
    size: Size,
    reach: Float,
    layoutDirection: LayoutDirection,
): Pair<Offset, Offset> {
    val y = size.height / 2
    val r = reach.coerceIn(0f, 1f)
    return if (layoutDirection == LayoutDirection.Ltr) {
        Offset(0f, y) to Offset(size.width * r, y)
    } else {
        Offset(size.width, y) to Offset(size.width * (1f - r), y)
    }
}

/** A line that draws across the middle of the element, holds for [hold], then fades out. */
internal class StrikeRunner(
    private val color: Color,
    private val width: Dp,
    private val hold: Duration,
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
        // Butt ends inside the element, a round cap on the moving end.
        val (start, end) = strikeLine(size, r, layoutDirection)
        drawLine(color, start, end, stroke, alpha = a)
        if (r < 1f) drawCircle(color, stroke / 2, end, alpha = a)
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        job =
            context.scope.launch {
                alpha.snapTo(1f)
                if (context.isReducedMotion()) {
                    // The line appears whole instead of drawing across.
                    reach.snapTo(1f)
                } else {
                    reach.snapTo(0f)
                    reach.animateTo(1f, tween(DRAW_MILLIS, easing = FastOutSlowInEasing))
                }
                awaitFrameTime(hold)
                alpha.animateTo(0f, tween(FADE_MILLIS))
                reach.snapTo(0f)
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        const val DRAW_MILLIS = 260
        const val FADE_MILLIS = 300
    }
}
