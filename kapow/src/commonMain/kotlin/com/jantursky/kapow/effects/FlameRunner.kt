package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
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
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** A few puffs from the middle of the top edge of an element [width] px wide, rising about [height] px. */
internal fun flameEmission(
    width: Float,
    height: Float,
): Emission =
    Emission(
        count = 3,
        angle = Spread(-90f - 6f, -90f + 6f),
        speed = Spread(height * 0.9f, height * 1.4f),
        lifetime = Spread(0.45f, 0.75f),
        gravity = -height,
        scale = Spread(0.75f, 1.1f),
        offsetX = Spread(-minOf(width * 0.22f, height * 0.5f), minOf(width * 0.22f, height * 0.5f)),
        offsetY = Spread(height * 0.08f),
        stagger = 0.02f,
        wobble = height * 0.05f,
        wobbleRate = 3f,
    )

/**
 * The flame painter only learns a puff's opacity, so the batch hands it the life left instead (1 to 0, see
 * [flameLife]) and the painter turns that into the color and the opacity it really draws with.
 */
internal fun flameLife(progress: Float): Float = 1f - progress.coerceIn(0f, 1f)

/** A puff's color at [progress] through its life, along [colors] from the first to the last. */
internal fun flameColor(
    colors: List<Color>,
    progress: Float,
): Color {
    val p = progress.coerceIn(0f, 1f) * (colors.size - 1)
    val i = p.toInt().coerceAtMost(colors.size - 2)
    return lerp(colors[i], colors[i + 1], p - i)
}

/** A puff's opacity at [progress]: it flares up fast, then burns out. */
internal fun flameOpacity(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    val rise = (p / 0.1f).coerceAtMost(1f)
    return rise * (1f - p * p)
}

/** Puffs shrink as they rise, so the flame narrows to a tip. */
internal fun flameGrowth(progress: Float): Float = 1f - 0.7f * progress.coerceIn(0f, 1f)

/** A soft round puff whose color and opacity follow its life, from radial gradients built once. */
internal fun flamePainter(
    colors: List<Color>,
    radiusPx: Float,
): ParticlePainter {
    val brushes =
        Array(FLAME_SHADES) { k ->
            val color = flameColor(colors, k / (FLAME_SHADES - 1f))
            Brush.radialGradient(
                0f to color,
                0.45f to color.copy(alpha = color.alpha * 0.85f),
                1f to Color.Transparent,
                center = Offset.Zero,
                radius = radiusPx,
            )
        }
    return ParticlePainter { _, life ->
        val progress = 1f - life
        val alpha = flameOpacity(progress)
        if (alpha > 0f) {
            // Taller than wide, so the overlapping puffs read as tongues rather than a cloud.
            scale(1f, FLAME_STRETCH, pivot = Offset.Zero) {
                drawCircle(brushes[(progress * (FLAME_SHADES - 1)).roundToInt()], radiusPx, Offset.Zero, alpha)
            }
        }
    }
}

private const val FLAME_SHADES = 24
private const val FLAME_STRETCH = 1.6f

/** Procedural flames above the element while active. Reduced motion shows none. */
internal class FlameRunner(
    private val colors: List<Color>,
    private val height: Dp,
) : ConditionalRunner() {
    private var job: Job? = null
    private var seed = 0

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        job?.cancel()
        if (!active || context.isReducedMotion()) return
        val reach = with(context.density) { height.toPx() }
        val painter = flamePainter(colors, reach * 0.22f)
        job =
            context.scope.launch {
                while (isActive) {
                    val width = context.size.width.toFloat()
                    val batch =
                        ParticleBatch(
                            ParticleSystem(flameEmission(width, reach), seed++),
                            painter,
                            opacity = ::flameLife,
                            growth = ::flameGrowth,
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
        val INTERVAL = 30.milliseconds
    }
}
