package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ParticlePhysics
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.applyTo
import com.jantursky.kapow.internal.growthCurve
import com.jantursky.kapow.internal.opacityCurve
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.pickColors
import com.jantursky.kapow.particles.shapePainter
import com.jantursky.kapow.transitions.popScale
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** How long the large heart lasts, in seconds. */
internal const val HEARTS_BIG_LIFETIME = 0.8f

/** How long the heart of reduced motion takes to fade in and out, in seconds. */
internal const val HEARTS_REDUCED_LIFETIME = 0.6f

/** The size of the large heart before [scale]: 70 % of the element's shorter side, but at least 28 dp. */
internal fun bigHeartSize(
    size: Size,
    scale: Float,
    density: Density,
): Float = max(min(size.width, size.height) * 0.7f, with(density) { 28.dp.toPx() }) * scale

/** The size factor of the large heart at [progress] of its life: up from nothing past full size and back to it. */
internal fun bigHeartGrowth(
    progress: Float,
    overshoot: Float,
): Float = popScale(progress * 2.2f, overshoot)

/** The large heart is fully visible for the first half of its life, then fades out. */
internal fun bigHeartOpacity(progress: Float): Float = if (progress < 0.5f) 1f else 1f - (progress - 0.5f) / 0.5f

/** The heart of reduced motion fades in and out in place: invisible at both ends, fully visible in the middle. */
internal fun reducedHeartOpacity(progress: Float): Float = sin(PI.toFloat() * progress.coerceIn(0f, 1f))

/** [count] small hearts that start across the middle of the element and drift upward with a little sway. */
internal fun smallHeartsEmission(
    count: Int,
    width: Float,
    density: Density,
): Emission =
    with(density) {
        Emission(
            count = count,
            angle = Spread(-90f - 35f, -90f + 35f),
            speed = Spread(70.dp.toPx(), 150.dp.toPx()),
            lifetime = Spread(0.9f, 1.4f),
            gravity = -50.dp.toPx(),
            drag = 0.45f,
            spin = Spread(-40f, 40f),
            rotation = Spread(-20f, 20f),
            scale = Spread(0.6f, 1f),
            offsetX = Spread(-width / 4, width / 4),
            stagger = 0.18f,
            wobble = 5.dp.toPx(),
            wobbleRate = 1.1f,
        )
    }

/** A large heart that pops over the element and small hearts that float up from it. */
internal class HeartsRunner(
    private val color: Color,
    private val count: Int,
    private val physics: ParticlePhysics = ParticlePhysics.Default,
    private val bigHeart: Boolean = true,
    private val bigHeartScale: Float = 1f,
    private val overshoot: Float = 1f,
    private val smallColors: List<Color> = emptyList(),
    private val heartSize: Dp = 14.dp,
    private val origin: TransformOrigin = TransformOrigin.Center,
    firstSeed: Int = 0,
) : ChangeRunner() {
    private var seed = firstSeed

    override fun fire(context: EffectContext) {
        val size = Size(context.size.width.toFloat(), context.size.height.toFloat())
        val at = Offset(size.width * origin.pivotFractionX, size.height * origin.pivotFractionY)
        val bigPx = bigHeartSize(size, bigHeartScale, context.density)
        if (context.isReducedMotion()) {
            if (context.showsReducedStandIn()) {
                emitBig(context, at, bigPx, HEARTS_REDUCED_LIFETIME, ::reducedHeartOpacity) { 1f }
            }
            return
        }
        if (count > 0) emitSmall(context, size, at)
        if (bigHeart) {
            emitBig(context, at, bigPx, HEARTS_BIG_LIFETIME, ::bigHeartOpacity) { bigHeartGrowth(it, overshoot) }
        }
    }

    private fun emitSmall(
        context: EffectContext,
        size: Size,
        at: Offset,
    ) {
        val emission =
            physics.applyTo(
                smallHeartsEmission(count, size.width, context.density),
                context.density.density,
                size,
                context.layoutDirection,
            )
        val colors = pickColors(smallColors.ifEmpty { listOf(color) }, physics.colorPick, count, seed)
        val px = with(context.density) { heartSize.toPx() }
        val batch =
            ParticleBatch(
                ParticleSystem(emission, seed++),
                shapePainter(ParticleShape.Heart, colors, px),
                opacity = physics.opacityCurve(::fadeOutLate),
                growth = physics.growthCurve(::growIn),
            )
        batch.origin = at
        batch.timeScale = context.speed
        batch.blendMode = physics.blendMode
        context.emit(batch)
    }

    private fun emitBig(
        context: EffectContext,
        at: Offset,
        sizePx: Float,
        lifetime: Float,
        opacity: (Float) -> Float,
        growth: (Float) -> Float,
    ) {
        val system = ParticleSystem(Emission(1, Spread(0f), Spread(0f), Spread(lifetime)), seed)
        val batch = ParticleBatch(system, shapePainter(ParticleShape.Heart, listOf(color), sizePx), opacity, growth)
        batch.origin = at
        batch.timeScale = context.speed
        context.emit(batch)
    }
}
