package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.puffPainter
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Two puffs from somewhere along the top edge of an element [width] px wide, drifting up and swaying, pushed
 * sideways by [wind] dp per second squared.
 */
internal fun smokeEmission(
    width: Float,
    density: Density,
    wind: Float = 0f,
): Emission =
    with(density) {
        Emission(
            count = 2,
            angle = Spread(-90f - 20f, -90f + 20f),
            speed = Spread(25.dp.toPx(), 55.dp.toPx()),
            lifetime = Spread(1.2f, 1.8f),
            gravity = -12.dp.toPx(),
            gravityX = wind.dp.toPx(),
            scale = Spread(0.6f, 1f),
            offsetX = Spread(-width * 0.35f, width * 0.35f),
            stagger = 0.06f,
            wobble = 5.dp.toPx(),
            wobbleRate = 0.8f,
        )
    }

/** Puffs fade in over the first fifth of their life, then thin out. */
internal fun puffOpacity(progress: Float): Float = puffOpacity(progress, PUFF_ALPHA)

/** Puffs fade in over the first fifth of their life, then thin out, at most [peak] opaque. */
internal fun puffOpacity(
    progress: Float,
    peak: Float,
): Float {
    val p = progress.coerceIn(0f, 1f)
    return peak * if (p < 0.2f) p / 0.2f else 1f - (p - 0.2f) / 0.8f
}

private const val PUFF_ALPHA = 0.7f

/** Puffs spread out as they rise. */
internal fun puffGrowth(progress: Float): Float = 0.5f + 1.1f * progress

/** Emits puffs from the top of the element while active. Reduced motion emits nothing. */
internal class SmokeRunner(
    private val color: Color,
    private val rate: Float = 1f,
    private val size: Dp = 10.dp,
    private val opacity: Float = PUFF_ALPHA,
    private val wind: Float = 0f,
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
        val painter = puffPainter(color, with(context.density) { size.toPx() })
        val fade = physics.opacityCurve { puffOpacity(it, opacity) }
        val grow = physics.growthCurve(::puffGrowth)
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
                                smokeEmission(width, context.density, wind),
                                context.density.density,
                                Size(width, height),
                                context.layoutDirection,
                            ).atSpeed(speed)
                    val batch = ParticleBatch(ParticleSystem(emission, seed++), painter, opacity = fade, growth = grow)
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
        val INTERVAL = 140.milliseconds
    }
}
