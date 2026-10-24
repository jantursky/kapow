package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticlePainter
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * [count] pieces of confetti thrown up and out from across the middle of an element [width] px wide, then pulled
 * down by gravity while drag slows them to a flutter.
 */
internal fun confettiEmission(
    count: Int,
    width: Float,
    density: Density,
): Emission =
    with(density) {
        Emission(
            count = count,
            angle = Spread(-90f - 65f, -90f + 65f),
            speed = Spread(380.dp.toPx(), 680.dp.toPx()),
            lifetime = Spread(1.2f, 1.7f),
            gravity = 700.dp.toPx(),
            drag = 0.2f,
            spin = Spread(-540f, 540f),
            rotation = Spread(0f, 360f),
            scale = Spread(0.7f, 1.1f),
            offsetX = Spread(-width / 4, width / 4),
            wobble = 5.dp.toPx(),
            wobbleRate = 1.4f,
        )
    }

/**
 * How wide a piece looks as it tumbles: its face turns towards and away from the viewer as it spins, so its width
 * swings between full and a thin edge. [phase] keeps neighbouring pieces out of step.
 */
internal fun confettiFlip(
    rotation: Float,
    phase: Float,
): Float = 0.15f + 0.85f * abs(cos(rotation * FLIP_RATE * PI.toFloat() / 180f + phase))

/** Pieces stay opaque for most of their life and fade over the last third. */
internal fun confettiOpacity(progress: Float): Float = if (progress < 0.66f) 1f else (1f - progress) / 0.34f

/** The size of piece [index]: every third one is a long thin ribbon, the rest small rectangles. */
internal fun confettiPiece(
    index: Int,
    unit: Float,
): Size = if (index % 3 == 2) Size(unit * 0.5f, unit * 2.2f) else Size(unit, unit * 1.4f)

private const val FLIP_RATE = 2.5f

/** A short pop of confetti from the element. With reduced motion the element briefly brightens instead. */
internal class ConfettiPopRunner(
    private val colors: List<Color>,
    private val count: Int,
) : ChangeRunner() {
    private val flash = Flash()
    private var flashJob: Job? = null
    private var seed = 0

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy = flash.compositingStrategy
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        flash.draw(this)
    }

    override fun fire(context: EffectContext) {
        if (context.isReducedMotion()) {
            flashJob?.cancel()
            flashJob = context.scope.launch { flash.play() }
            return
        }
        val width = context.size.width.toFloat()
        val system = ParticleSystem(confettiEmission(count, width, context.density), seed++)
        val unit = with(context.density) { 7.dp.toPx() }
        val painter =
            ParticlePainter { i, alpha ->
                val piece = confettiPiece(i, unit)
                val w = piece.width * confettiFlip(system.rotation[i], i * 1.7f)
                drawRect(colors[i % colors.size], Offset(-w / 2, -piece.height / 2), Size(w, piece.height), alpha)
            }
        val batch = ParticleBatch(system, painter, opacity = ::confettiOpacity)
        batch.origin = Offset(width / 2f, context.size.height / 2f)
        context.emit(batch)
    }

    override fun stop() {
        flashJob?.cancel()
    }
}
