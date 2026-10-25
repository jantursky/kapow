package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.atSpeed
import com.jantursky.kapow.internal.awaitFrameTime
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticlePainter
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.shapePainter
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

/**
 * One sparkle that stays put somewhere over an element [width] × [height] px, or up to [padding] px (by default
 * half a sparkle, [sizePx] across) past its edges, twinkling for a time in [lifetime] seconds.
 */
internal fun sparkleEmission(
    width: Float,
    height: Float,
    sizePx: Float,
    padding: Float = sizePx / 2,
    lifetime: ClosedFloatingPointRange<Float> = SPARKLE_LIFE_MIN..SPARKLE_LIFE_MAX,
): Emission =
    Emission(
        count = 1,
        angle = Spread(0f),
        speed = Spread(0f),
        lifetime = Spread(lifetime.start, lifetime.endInclusive),
        spin = Spread(-60f, 60f),
        rotation = Spread(-20f, 20f),
        scale = Spread(0.6f, 1f),
        offsetX = Spread(-width / 2 - padding, width / 2 + padding),
        offsetY = Spread(-height / 2 - padding, height / 2 + padding),
    )

/** How long to wait between sparkles so about [count] twinkle at once. */
internal fun sparkleInterval(
    count: Int,
    lifetime: ClosedFloatingPointRange<Float> = SPARKLE_LIFE_MIN..SPARKLE_LIFE_MAX,
): Float = (lifetime.start + lifetime.endInclusive) / 2 / count.coerceAtLeast(1)

/** A sparkle is bright for most of its twinkle, fading only as it gets small. */
internal fun twinkleOpacity(progress: Float): Float = sqrt(twinkle(progress))

/** A four-point star (or another [shape]) in [colors] used in turn, [sizePx] across, over a soft glow of its color. */
internal fun sparklePainter(
    colors: List<Color>,
    sizePx: Float,
    shape: ParticleShape = ParticleShape.Sparkle,
): ParticlePainter {
    val star = shapePainter(shape, colors, sizePx)
    val glows =
        colors.map {
            Brush.radialGradient(
                listOf(it.copy(alpha = it.alpha * 0.4f), Color.Transparent),
                Offset.Zero,
                sizePx * 0.7f,
            )
        }
    return ParticlePainter { i, alpha ->
        drawCircle(glows[i % glows.size], sizePx * 0.7f, Offset.Zero, alpha)
        with(star) { drawParticle(i, alpha) }
    }
}

private const val SPARKLE_LIFE_MIN = 0.8f
private const val SPARKLE_LIFE_MAX = 1.3f

/** Four-point stars that twinkle in and out over the element while active. Reduced motion shows none. */
internal class SparklesRunner(
    private val colors: List<Color>,
    private val size: Dp,
    private val count: Int,
    private val lifetime: ClosedFloatingPointRange<Float> = SPARKLE_LIFE_MIN..SPARKLE_LIFE_MAX,
    private val areaPadding: Dp = size / 2,
    private val shape: ParticleShape = ParticleShape.Sparkle,
    firstSeed: Int = 0,
) : ConditionalRunner() {
    private var job: Job? = null
    private var seed = firstSeed

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        job?.cancel()
        if (!active || context.isReducedMotion()) return
        val sizePx = with(context.density) { size.toPx() }
        val paddingPx = with(context.density) { areaPadding.toPx() }
        val painter = sparklePainter(colors, sizePx, shape)
        val speed = context.speed
        val interval = (sparkleInterval(count, lifetime) * 1000 / speed).toInt().milliseconds
        job =
            context.scope.launch {
                while (isActive) {
                    val width = context.size.width.toFloat()
                    val height = context.size.height.toFloat()
                    // Each sparkle has its own seed, so its color, spot and size differ from the last one.
                    val n = seed++
                    val batch =
                        ParticleBatch(
                            ParticleSystem(
                                sparkleEmission(width, height, sizePx, paddingPx, lifetime).atSpeed(speed),
                                n,
                            ),
                            ParticlePainter { i, alpha -> with(painter) { drawParticle(n + i, alpha) } },
                            opacity = ::twinkleOpacity,
                            growth = ::twinkle,
                        )
                    batch.origin = Offset(width / 2f, height / 2f)
                    context.emit(batch)
                    awaitFrameTime(interval)
                }
            }
    }

    override fun stop() {
        job?.cancel()
    }
}
