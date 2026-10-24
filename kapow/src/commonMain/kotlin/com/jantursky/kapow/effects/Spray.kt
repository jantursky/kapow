package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ParticleContent
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.painter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

/** One particle that drifts up from the top edge, swaying a little. */
internal fun riseEmission(density: Density): Emission =
    with(density) {
        Emission(
            count = 1,
            angle = Spread(-90f),
            speed = Spread(70.dp.toPx()),
            lifetime = Spread(1.2f),
            drag = 0.3f,
            wobble = 4.dp.toPx(),
            wobbleRate = 1.2f,
        )
    }

/** Particles grow in over the first 15 % of their life. */
internal fun growIn(progress: Float): Float {
    val t = (progress / 0.15f).coerceIn(0f, 1f)
    return 0.4f + 0.6f * (1f - (1f - t) * (1f - t))
}

/** Particles stay opaque for the first half of their life, then fade out. */
internal fun fadeOutLate(progress: Float): Float = if (progress < 0.5f) 1f else 1f - (progress - 0.5f) / 0.5f

/** Shared by spray and rise: emits a batch on each change, or flashes the element with reduced motion. */
internal abstract class ParticleChangeRunner(
    private val particles: ParticleContent,
) : ChangeRunner() {
    private val flash = Flash()
    private var flashJob: Job? = null
    private var textMeasurer: TextMeasurer? = null

    // Each emission gets the next seed: different from the last, the same from run to run.
    private var seed = 0

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy = flash.compositingStrategy
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        flash.draw(this)
    }

    protected abstract fun emission(density: Density): Emission

    /** Where the particles start, relative to the element's top left corner. */
    protected abstract fun origin(context: EffectContext): Offset

    override fun fire(context: EffectContext) {
        if (context.isReducedMotion()) {
            flashJob?.cancel()
            flashJob = context.scope.launch { flash.play() }
            return
        }
        val painter =
            particles.painter(context.density) {
                textMeasurer
                    ?: TextMeasurer(context.fontFamilyResolver, context.density, context.layoutDirection)
                        .also { textMeasurer = it }
            }
        val batch =
            ParticleBatch(
                ParticleSystem(emission(context.density), seed++),
                painter,
                opacity = ::fadeOutLate,
                growth = ::growIn,
            )
        batch.origin = origin(context)
        context.emit(batch)
    }

    override fun stop() {
        flashJob?.cancel()
    }
}

internal class SprayRunner(
    particles: ParticleContent,
    private val count: Int,
) : ParticleChangeRunner(particles) {
    override fun emission(density: Density): Emission = sprayEmission(count, density)

    override fun origin(context: EffectContext): Offset = Offset(context.size.width / 2f, context.size.height / 2f)
}

internal class RiseRunner(
    particles: ParticleContent,
) : ParticleChangeRunner(particles) {
    override fun emission(density: Density): Emission = riseEmission(density)

    override fun origin(context: EffectContext): Offset = Offset(context.size.width / 2f, 0f)
}
