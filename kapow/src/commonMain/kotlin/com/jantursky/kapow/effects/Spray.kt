package com.jantursky.kapow.effects

import androidx.compose.animation.core.Easing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ParticleContent
import com.jantursky.kapow.ParticlePhysics
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.applyTo
import com.jantursky.kapow.internal.at
import com.jantursky.kapow.internal.growthCurve
import com.jantursky.kapow.internal.opacityCurve
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.painter
import com.jantursky.kapow.particles.picked
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.ln
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** A fan of [count] particles thrown upward from the element's center, braked hard by drag and pulled back down. */
internal fun sprayEmission(
    count: Int,
    density: Density,
): Emission =
    with(density) {
        Emission(
            count = count,
            angle = Spread(-90f - 50f, -90f + 50f),
            speed = Spread(380.dp.toPx(), 620.dp.toPx()),
            lifetime = Spread(0.8f, 1.1f),
            gravity = 600.dp.toPx(),
            drag = 0.12f,
            spin = Spread(-180f, 180f),
            rotation = Spread(-30f, 30f),
            scale = Spread(0.6f, 1f),
            offsetX = Spread(-6.dp.toPx(), 6.dp.toPx()),
            offsetY = Spread(-6.dp.toPx(), 6.dp.toPx()),
        )
    }

/** The rise effect's default drag, as the fraction of speed kept after one second. */
internal const val RISE_DRAG = 0.3f

/** The rise effect's default launch speed, in dp per second. */
internal const val RISE_SPEED = 70f

/**
 * The launch speed, in the same unit as [distance], that carries a particle [distance] before it expires after
 * [lifetime] seconds, given that it keeps [drag] of its speed each second.
 */
internal fun riseSpeedFor(
    distance: Float,
    lifetime: Float,
    drag: Float = RISE_DRAG,
): Float = distance * -ln(drag) / (1f - exp(ln(drag) * lifetime))

/**
 * One particle that drifts from the top edge, swaying a little. [speed] and [lifetime] default to the standard
 * rise; [direction] is the heading in degrees (-90 is straight up).
 */
internal fun riseEmission(
    density: Density,
    speed: Float = RISE_SPEED,
    lifetime: Float = 1.2f,
    direction: Float = -90f,
    wobble: Dp = 4.dp,
): Emission =
    with(density) {
        Emission(
            count = 1,
            angle = Spread(direction),
            speed = Spread(speed.dp.toPx()),
            lifetime = Spread(lifetime),
            drag = RISE_DRAG,
            wobble = wobble.toPx(),
            wobbleRate = 1.2f,
        )
    }

/**
 * A particle that grows in from [from] times its size over the first 15 % of its life. With [from] 0.4 this is
 * the standard grow-in.
 */
internal fun growInFrom(
    from: Float,
    progress: Float,
): Float {
    val t = (progress / 0.15f).coerceIn(0f, 1f)
    return from + (1f - from) * (1f - (1f - t) * (1f - t))
}

/** Particles grow in over the first 15 % of their life. */
internal fun growIn(progress: Float): Float = growInFrom(0.4f, progress)

/** Particles stay opaque for the first half of their life, then fade out. */
internal fun fadeOutLate(progress: Float): Float = if (progress < 0.5f) 1f else 1f - (progress - 0.5f) / 0.5f

/** Shared by spray and rise: emits a batch on each change, or flashes the element with reduced motion. */
internal abstract class ParticleChangeRunner(
    private val particles: ParticleContent,
    private val physics: ParticlePhysics,
    firstSeed: Int,
) : ChangeRunner() {
    private val flash = Flash()
    private var flashJob: Job? = null
    private var textMeasurer: TextMeasurer? = null

    // Each emission gets the next seed: different from the last, the same from run to run.
    private var seed = firstSeed

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy = flash.compositingStrategy
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        flash.draw(this)
    }

    /** The effect's own emission, before [physics] is laid over it. */
    protected abstract fun emission(context: EffectContext): Emission

    /** Where the particles start, relative to the element's top left corner. */
    protected abstract fun origin(context: EffectContext): Offset

    /** The particles to draw for the change that just happened. */
    internal open fun content(value: Any?): ParticleContent = particles

    protected open val opacity: (Float) -> Float get() = ::fadeOutLate

    protected open val growth: (Float) -> Float get() = ::growIn

    override fun fire(context: EffectContext) {
        if (context.isReducedMotion()) {
            flashJob?.cancel()
            if (context.showsReducedStandIn()) flashJob = context.scope.launch { flash.play(context) }
            return
        }
        val area = Size(context.size.width.toFloat(), context.size.height.toFloat())
        val emission =
            physics.applyTo(emission(context), context.density.density, area, context.layoutDirection)
        val shown = content(changedValue).picked(physics.colorPick, emission.count, seed)
        val painter =
            shown.painter(context.density) {
                textMeasurer
                    ?: TextMeasurer(context.fontFamilyResolver, context.density, context.layoutDirection)
                        .also { textMeasurer = it }
            }
        val batch =
            ParticleBatch(
                ParticleSystem(emission, seed++),
                painter,
                opacity = physics.opacityCurve(opacity),
                growth = physics.growthCurve(growth),
            )
        batch.origin = origin(context)
        batch.timeScale = context.speed
        batch.blendMode = physics.blendMode
        context.emit(batch)
    }

    override fun stop() {
        flashJob?.cancel()
    }
}

internal class SprayRunner(
    particles: ParticleContent,
    private val count: Int,
    physics: ParticlePhysics = ParticlePhysics.Default,
    private val origin: TransformOrigin = TransformOrigin.Center,
    private val rotation: ClosedFloatingPointRange<Float> = -30f..30f,
    seed: Int = 0,
) : ParticleChangeRunner(particles, physics, seed) {
    override fun emission(context: EffectContext): Emission =
        sprayEmission(count, context.density).copy(rotation = Spread(rotation.start, rotation.endInclusive))

    override fun origin(context: EffectContext): Offset =
        Offset(context.size.width * origin.pivotFractionX, context.size.height * origin.pivotFractionY)
}

internal class RiseRunner(
    particles: ParticleContent,
    private val distance: Dp = Dp.Unspecified,
    private val duration: Duration = 1200.milliseconds,
    private val drift: Float = 0f,
    private val wobble: Dp = 4.dp,
    private val startOffset: DpOffset = DpOffset.Zero,
    private val scaleIn: Float = 0.4f,
    private val scaleOut: Float = 1f,
    private val fade: Easing? = null,
    private val label: ((Any?) -> String)? = null,
    private val anchor: TransformOrigin = TransformOrigin(0.5f, 0f),
    physics: ParticlePhysics = ParticlePhysics.Default,
) : ParticleChangeRunner(particles, physics, 0) {
    private val lifetime = duration.inWholeMilliseconds / 1000f

    override fun emission(context: EffectContext): Emission {
        val speed =
            if (distance == Dp.Unspecified) RISE_SPEED else riseSpeedFor(distance.value, lifetime)
        return riseEmission(context.density, speed, lifetime, -90f + drift, wobble)
    }

    override fun origin(context: EffectContext): Offset =
        with(context.density) {
            Offset(
                context.size.width * anchor.pivotFractionX + startOffset.x.toPx(),
                context.size.height * anchor.pivotFractionY + startOffset.y.toPx(),
            )
        }

    override fun content(value: Any?): ParticleContent {
        val make = label
        val base = super.content(value)
        return if (make != null && base is ParticleContent.Text) base.copy(text = make(value)) else base
    }

    override val opacity: (Float) -> Float
        get() = fade?.let { easing -> { 1f - easing.transform(it.coerceIn(0f, 1f)) } } ?: ::fadeOutLate

    override val growth: (Float) -> Float
        get() = { growInFrom(scaleIn, it) * (1f + (scaleOut - 1f) * it) }
}
