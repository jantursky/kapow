package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.EdgeSide
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
import com.jantursky.kapow.particles.ParticlePainter
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * A few puffs from the middle of an element's edge that is [width] px long, rising about [height] px away from it.
 * [edge] says which edge burns, [flameWidth] px (or by default about a fifth of the edge) is the width of the
 * base, [wind] pushes the flames sideways in px per second squared and [flicker] speeds up their sway.
 */
internal fun flameEmission(
    width: Float,
    height: Float,
    edge: EdgeSide = EdgeSide.Top,
    flameWidth: Float? = null,
    wind: Float = 0f,
    flicker: Float = 1f,
    rtl: Boolean = false,
): Emission {
    val half = if (flameWidth != null) flameWidth / 2f else minOf(width * 0.22f, height * 0.5f)
    val base = height * 0.08f
    val vertical = edge == EdgeSide.Top || edge == EdgeSide.Bottom
    val left = (edge == EdgeSide.Start) != rtl && !vertical
    val angle =
        when {
            edge == EdgeSide.Top -> -90f
            edge == EdgeSide.Bottom -> 90f
            left -> 180f
            else -> 0f
        }
    return Emission(
        count = 3,
        angle = Spread(angle - 6f, angle + 6f),
        speed = Spread(height * 0.9f, height * 1.4f),
        lifetime = Spread(0.45f, 0.75f),
        gravity = if (vertical) (if (edge == EdgeSide.Top) -height else height) else wind,
        gravityX = if (vertical) wind else (if (left) -height else height),
        scale = Spread(0.75f, 1.1f),
        offsetX = if (vertical) Spread(-half, half) else Spread(if (left) base else -base),
        offsetY =
            if (vertical) {
                Spread(if (edge == EdgeSide.Top) base else -base)
            } else {
                Spread(-half, half)
            },
        stagger = 0.02f,
        wobble = if (vertical) height * 0.05f else 0f,
        wobbleRate = 3f * flicker,
    )
}

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
    sideways: Boolean = false,
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
            scale(if (sideways) FLAME_STRETCH else 1f, if (sideways) 1f else FLAME_STRETCH, pivot = Offset.Zero) {
                drawCircle(brushes[(progress * (FLAME_SHADES - 1)).roundToInt()], radiusPx, Offset.Zero, alpha)
            }
        }
    }
}

private const val FLAME_SHADES = 24
private const val FLAME_STRETCH = 1.6f

/** Procedural flames along an edge of the element while active. Reduced motion shows none. */
internal class FlameRunner(
    private val colors: List<Color>,
    private val height: Dp,
    private val width: Dp? = null,
    private val intensity: Float = 1f,
    private val wind: Float = 0f,
    private val edge: EdgeSide = EdgeSide.Top,
    private val flicker: Float = 1f,
    private val physics: ParticlePhysics = ParticlePhysics.Default,
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
        val reach = with(context.density) { height.toPx() }
        val baseWidth = with(context.density) { width?.toPx() }
        val windPx = with(context.density) { wind.dp.toPx() }
        val vertical = edge == EdgeSide.Top || edge == EdgeSide.Bottom
        val painter = flamePainter(colors, reach * 0.22f, sideways = !vertical)
        val fade = physics.opacityCurve(::flameLife)
        val grow = physics.growthCurve(::flameGrowth)
        val speed = context.speed
        val interval = scaledInterval(INTERVAL, intensity * speed)
        val rtl = context.layoutDirection == LayoutDirection.Rtl
        job =
            context.scope.launch {
                while (isActive) {
                    val w = context.size.width.toFloat()
                    val h = context.size.height.toFloat()
                    val emission =
                        physics
                            .applyTo(
                                flameEmission(if (vertical) w else h, reach, edge, baseWidth, windPx, flicker, rtl),
                                context.density.density,
                                Size(w, h),
                                context.layoutDirection,
                            ).atSpeed(speed)
                    val batch = ParticleBatch(ParticleSystem(emission, seed++), painter, opacity = fade, growth = grow)
                    val start = (edge == EdgeSide.Start) != rtl
                    batch.origin =
                        physics.emitterOrigin(
                            w,
                            h,
                            when {
                                edge == EdgeSide.Top -> Offset(w / 2f, 0f)
                                edge == EdgeSide.Bottom -> Offset(w / 2f, h)
                                start -> Offset(0f, h / 2f)
                                else -> Offset(w, h / 2f)
                            },
                        )
                    context.emit(batch)
                    awaitFrameTime(interval)
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
