package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ParticlePhysics
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.applyTo
import com.jantursky.kapow.internal.atSpeed
import com.jantursky.kapow.internal.awaitFrameTime
import com.jantursky.kapow.internal.emitterOrigin
import com.jantursky.kapow.internal.growthCurve
import com.jantursky.kapow.internal.opacityCurve
import com.jantursky.kapow.internal.scaledInterval
import com.jantursky.kapow.internal.scatter
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticlePainter
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * One bubble from somewhere along the top edge of an element [width] px wide, rising faster as it goes and
 * swaying [wobble] wide.
 */
internal fun bubbleEmission(
    width: Float,
    density: Density,
    wobble: Dp = 3.dp,
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
            wobble = wobble.toPx(),
            wobbleRate = 1.6f,
        )
    }

/**
 * A bubble appears quickly, stays clear, then pops: it fades out over the last tenth of its life. Without [pop] it
 * thins out gently over the last third instead.
 */
internal fun bubbleOpacity(
    progress: Float,
    pop: Boolean = true,
): Float {
    val p = progress.coerceIn(0f, 1f)
    val end = if (pop) POP else FADE
    return when {
        p < 0.1f -> p / 0.1f
        p < end -> 1f
        else -> 1f - (p - end) / (1f - end)
    }
}

/** A bubble grows a little as it rises and swells as it pops. Without [pop] it only grows a little. */
internal fun bubbleGrowth(
    progress: Float,
    pop: Boolean = true,
): Float {
    val p = progress.coerceIn(0f, 1f)
    val rise = 0.7f + 0.3f * (p / POP).coerceAtMost(1f)
    return if (p < POP || !pop) rise else rise + 0.6f * (p - POP) / (1f - POP)
}

private const val POP = 0.9f
private const val FADE = 0.67f

/** How many differently tinted painters a [bubbleVariants] set holds. */
private const val VARIANTS = 8

/**
 * [VARIANTS] painters for bubbles in [color], each pushed toward white or black by up to [variation] (0 to 1) so
 * the bubbles are not all the same. Without variation there is a single painter.
 */
internal fun bubbleVariants(
    color: Color,
    radiusPx: Float,
    variation: Float,
): List<ParticlePainter> {
    if (variation <= 0f) return listOf(bubblePainter(color, radiusPx))
    return List(VARIANTS) { k ->
        val shift = (k / (VARIANTS - 1f) * 2f - 1f) * variation
        val tinted =
            if (shift >=
                0f
            ) {
                lerp(color, Color.White, shift * 0.6f)
            } else {
                lerp(color, Color.Black, -shift * 0.6f)
            }
        bubblePainter(tinted, radiusPx)
    }
}

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
    private val rate: Float = 1f,
    private val wobble: Dp = 3.dp,
    private val pop: Boolean = true,
    private val colorVariation: Float = 0f,
    private val physics: ParticlePhysics = ParticlePhysics.Default,
    private val firstSeed: Int = 0,
) : ConditionalRunner() {
    private var job: Job? = null
    private var seed = firstSeed

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        job?.cancel()
        if (!active || context.isReducedMotion()) return
        val variants = bubbleVariants(color, with(context.density) { size.toPx() } / 2, colorVariation)
        val fade = physics.opacityCurve { bubbleOpacity(it, pop) }
        val grow = physics.growthCurve { bubbleGrowth(it, pop) }
        val speed = context.speed
        val interval = scaledInterval(INTERVAL, rate * speed)
        job =
            context.scope.launch {
                while (isActive) {
                    val width = context.size.width.toFloat()
                    val height = context.size.height.toFloat()
                    val emission =
                        physics
                            .applyTo(
                                bubbleEmission(width, context.density, wobble),
                                context.density.density,
                                Size(width, height),
                                context.layoutDirection,
                            ).atSpeed(speed)
                    val n = seed++
                    val painter =
                        if (variants.size == 1) {
                            variants[0]
                        } else {
                            variants[scatter(firstSeed, n, variants.size)]
                        }
                    val batch = ParticleBatch(ParticleSystem(emission, n), painter, opacity = fade, growth = grow)
                    batch.origin = physics.emitterOrigin(width, height, Offset(width / 2f, 0f))
                    context.emit(batch)
                    awaitFrameTime(interval)
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
