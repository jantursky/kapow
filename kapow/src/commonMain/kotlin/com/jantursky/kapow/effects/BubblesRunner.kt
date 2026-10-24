package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.awaitFrameTime
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticlePainter
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** One bubble from somewhere along the top edge of an element [width] px wide, rising faster as it goes. */
internal fun bubbleEmission(
    width: Float,
    density: Density,
): Emission =
    with(density) {
        Emission(
            count = 1,
            angle = Spread(-90f - 6f, -90f + 6f),
            speed = Spread(14.dp.toPx(), 26.dp.toPx()),
            lifetime = Spread(0.9f, 1.5f),
            gravity = -16.dp.toPx(),
            scale = Spread(0.5f, 1f),
            offsetX = Spread(-width * 0.42f, width * 0.42f),
            offsetY = Spread(4.dp.toPx()),
            wobble = 3.dp.toPx(),
            wobbleRate = 1.6f,
        )
    }

/** A bubble appears quickly, stays clear, then pops: it fades out over the last tenth of its life. */
internal fun bubbleOpacity(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return when {
        p < 0.1f -> p / 0.1f
        p < POP -> 1f
        else -> 1f - (p - POP) / (1f - POP)
    }
}

/** A bubble grows a little as it rises and swells as it pops. */
internal fun bubbleGrowth(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    val rise = 0.7f + 0.3f * (p / POP).coerceAtMost(1f)
    return if (p < POP) rise else rise + 0.6f * (p - POP) / (1f - POP)
}

private const val POP = 0.9f

/** A see-through bubble: a thin ring in [color], a faint fill and a highlight near its top left. */
internal fun bubblePainter(
    color: Color,
    radiusPx: Float,
): ParticlePainter {
    val ring = Stroke(radiusPx * 0.18f)
    val shine = Offset(-radiusPx * 0.38f, -radiusPx * 0.38f)
    return ParticlePainter { _, alpha ->
        drawCircle(color, radiusPx, Offset.Zero, alpha * 0.18f)
        drawCircle(color, radiusPx, Offset.Zero, alpha * 0.9f, style = ring)
        drawCircle(Color.White, radiusPx * 0.22f, shine, alpha * 0.8f)
    }
}

/** Lets bubbles rise from the top of the element while active. Reduced motion emits none. */
internal class BubblesRunner(
    private val color: Color,
    private val size: Dp,
) : ConditionalRunner() {
    private var job: Job? = null
    private var seed = 0

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        job?.cancel()
        if (!active || context.isReducedMotion()) return
        val painter = bubblePainter(color, with(context.density) { size.toPx() } / 2)
        job =
            context.scope.launch {
                while (isActive) {
                    val width = context.size.width.toFloat()
                    val batch =
                        ParticleBatch(
                            ParticleSystem(bubbleEmission(width, context.density), seed++),
                            painter,
                            opacity = ::bubbleOpacity,
                            growth = ::bubbleGrowth,
                        )
                    batch.origin = Offset(width / 2f, 0f)
                    context.emit(batch)
                    awaitFrameTime(INTERVAL)
                }
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        val INTERVAL = 160.milliseconds
    }
}
