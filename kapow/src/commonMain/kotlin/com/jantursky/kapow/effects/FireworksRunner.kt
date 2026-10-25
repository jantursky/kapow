package com.jantursky.kapow.effects

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.lerp
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
import com.jantursky.kapow.particles.ParticlePainter
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.pickColors
import com.jantursky.kapow.particles.puffPainter
import com.jantursky.kapow.particles.shapePainter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How long the rocket climbs, in seconds. */
internal const val RISE_SECONDS = 0.45f

/**
 * The rocket: one spark shot straight up and slowed by gravity until it stops [height] px above where it started,
 * after [seconds] ([RISE_SECONDS] by default). That is where it ends its life and the explosion takes over.
 */
internal fun rocketEmission(
    height: Float,
    seconds: Float = RISE_SECONDS,
): Emission {
    val speed = 2 * height / seconds
    return Emission(
        count = 1,
        angle = Spread(-90f),
        speed = Spread(speed),
        lifetime = Spread(seconds),
        gravity = speed / seconds,
    )
}

/** How far up the rocket is at [progress] of its climb, as a fraction of its height. It slows to a stop at the top. */
internal fun rocketRise(progress: Float): Float {
    val rest = 1f - progress.coerceIn(0f, 1f)
    return 1f - rest * rest
}

/** Two embers that drop off the rocket where it is and fall away behind it, leaving a trail. */
internal fun emberEmission(
    density: Density,
    trail: Float = 1f,
): Emission =
    with(density) {
        Emission(
            count = 2,
            angle = Spread(60f, 120f),
            speed = Spread(10.dp.toPx(), 30.dp.toPx()),
            lifetime = Spread(0.25f * trail, 0.4f * trail),
            gravity = 60.dp.toPx(),
            scale = Spread(0.5f, 0.9f),
        )
    }

/** The explosion: [count] sparks thrown out in a ring at nearly the same speed, braked by the air and falling. */
internal fun explosionEmission(
    count: Int,
    density: Density,
    reach: Float = 1f,
): Emission =
    with(density) {
        Emission(
            count = count,
            angle = Spread(0f, 360f),
            speed = Spread(180.dp.toPx() * reach, 210.dp.toPx() * reach),
            lifetime = Spread(0.9f, 1.2f),
            gravity = 170.dp.toPx(),
            drag = 0.2f,
            scale = Spread(0.7f, 1f),
        )
    }

/** Explosion sparks burn bright for the first half of their life, then fade out. */
internal fun sparkOpacity(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return if (p < 0.5f) 1f else 1f - (p - 0.5f) / 0.5f
}

/** Explosion sparks burn down to 60 % of their size. */
internal fun sparkGrowth(progress: Float): Float = 1f - 0.4f * progress.coerceIn(0f, 1f)

/** A rocket that climbs from the element's top edge and bursts into a ring of falling sparks. */
internal class FireworksRunner(
    private val colors: List<Color>,
    private val count: Int,
    private val rocketHeight: Dp = 100.dp,
    private val rocketDuration: Duration = 450.milliseconds,
    private val burstRadius: Dp = Dp.Unspecified,
    private val trailLength: Float = 1f,
    private val particleSize: Dp = 7.dp,
    private val origin: TransformOrigin = TransformOrigin(0.5f, 0f),
    private val physics: ParticlePhysics = ParticlePhysics.Default,
    firstSeed: Int = 0,
) : ChangeRunner() {
    private val flash = Flash()
    private var job: Job? = null

    // Every emission gets the next seed: a different explosion from the last, the same from run to run.
    private var seed = firstSeed

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy = flash.compositingStrategy
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        flash.draw(this)
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        if (context.isReducedMotion()) {
            if (context.showsReducedStandIn()) job = context.scope.launch { flash.play(context) }
            return
        }
        val launch = Offset(context.size.width * origin.pivotFractionX, context.size.height * origin.pivotFractionY)
        val area = Size(context.size.width.toFloat(), context.size.height.toFloat())
        val speed = context.speed
        val rise = rocketDuration.inWholeMilliseconds / 1000f
        val riseNanos = rocketDuration.inWholeNanoseconds
        job =
            context.scope.launch {
                with(context.density) {
                    val height = rocketHeight.toPx()
                    val rocketColor = lerp(colors.first(), Color.White, 0.35f)
                    val size = particleSize.toPx()
                    val rocket = shapePainter(ParticleShape.Circle, listOf(rocketColor), size)
                    emit(context, ParticleSystem(rocketEmission(height, rise), seed++), rocket, launch) { 1f }
                    val ember = shapePainter(ParticleShape.Circle, listOf(rocketColor), size * 4f / 7f)
                    val start = withFrameNanos { it }
                    var nextEmber = 0L
                    while (true) {
                        // The climb is timed on the frame clock, so a faster effect gets there sooner.
                        val elapsed = ((withFrameNanos { it } - start) * speed.toDouble()).toLong()
                        if (elapsed >= riseNanos) break
                        if (trailLength <= 0f || elapsed < nextEmber) continue
                        nextEmber += EMBER_NANOS
                        val at = launch + Offset(0f, -height * rocketRise(elapsed / riseNanos.toFloat()))
                        emit(context, ParticleSystem(emberEmission(this, trailLength), seed++), ember, at) { 1f - it }
                    }
                    val top = launch + Offset(0f, -height)
                    val glare = puffPainter(Color.White.copy(alpha = 0.8f), 28.dp.toPx())
                    emit(context, ParticleSystem(GLARE, seed++), glare, top, growth = { 0.6f + 0.8f * it }) { 1f - it }
                    val reach = if (burstRadius == Dp.Unspecified) 1f else burstRadius.value / BURST_REACH
                    val emission =
                        physics.applyTo(explosionEmission(count, this, reach), density, area, context.layoutDirection)
                    val sparks =
                        shapePainter(ParticleShape.Circle, pickColors(colors, physics.colorPick, count, seed), size)
                    val explosion = ParticleSystem(emission, seed++)
                    emit(
                        context,
                        explosion,
                        sparks,
                        top,
                        growth = physics.growthCurve(::sparkGrowth),
                        opacity = physics.opacityCurve(::sparkOpacity),
                        blendMode = physics.blendMode,
                    )
                }
            }
    }

    private fun emit(
        context: EffectContext,
        system: ParticleSystem,
        painter: ParticlePainter,
        origin: Offset,
        growth: (Float) -> Float = { 1f },
        blendMode: BlendMode? = null,
        opacity: (Float) -> Float,
    ) {
        val batch = ParticleBatch(system, painter, opacity, growth)
        batch.timeScale = context.speed
        batch.blendMode = blendMode
        batch.origin = origin
        context.emit(batch)
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        // About how far, in dp, the sparks of the standard explosion fly before they burn out.
        const val BURST_REACH = 100f
        const val EMBER_NANOS = 30_000_000L

        // A soft glare where the rocket bursts, gone in a moment.
        val GLARE = Emission(count = 1, angle = Spread(0f), speed = Spread(0f), lifetime = Spread(0.3f))
    }
}
