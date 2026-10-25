package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ParticlePhysics
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor

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

/** The size of piece [index]: by default every third one is a long thin ribbon, the rest small rectangles. */
internal fun confettiPiece(
    index: Int,
    unit: Float,
    ribbonRatio: Float = RIBBON_RATIO,
): Size = if (isRibbon(index, ribbonRatio)) Size(unit * 0.5f, unit * 2.2f) else Size(unit, unit * 1.4f)

/** Whether piece [index] is a ribbon when about [ratio] of all pieces are: 1/3 makes every third one. */
internal fun isRibbon(
    index: Int,
    ratio: Float,
): Boolean = floor((index + 1) * ratio + RIBBON_EPSILON) > floor(index * ratio + RIBBON_EPSILON)

private const val RIBBON_RATIO = 1f / 3f
private const val RIBBON_EPSILON = 1e-4f

private const val FLIP_RATE = 2.5f

/** A short pop of confetti from the element. With reduced motion the element briefly brightens instead. */
internal class ConfettiPopRunner(
    private val colors: List<Color>,
    private val count: Int,
    private val physics: ParticlePhysics = ParticlePhysics.Default,
    private val ribbonRatio: Float = RIBBON_RATIO,
    private val pieceSize: Dp = 7.dp,
    private val origin: TransformOrigin = TransformOrigin.Center,
    firstSeed: Int = 0,
) : ChangeRunner() {
    private val flash = Flash()
    private var flashJob: Job? = null
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
        if (context.isReducedMotion()) {
            flashJob?.cancel()
            if (context.showsReducedStandIn()) flashJob = context.scope.launch { flash.play(context) }
            return
        }
        val width = context.size.width.toFloat()
        val height = context.size.height.toFloat()
        val emission =
            physics.applyTo(
                confettiEmission(count, width, context.density),
                context.density.density,
                Size(width, height),
                context.layoutDirection,
            )
        val pieceColors = pickColors(colors, physics.colorPick, count, seed)
        val system = ParticleSystem(emission, seed++)
        val unit = with(context.density) { pieceSize.toPx() }
        val painter =
            ParticlePainter { i, alpha ->
                val piece = confettiPiece(i, unit, ribbonRatio)
                val w = piece.width * confettiFlip(system.rotation[i], i * 1.7f)
                drawRect(
                    pieceColors[i % pieceColors.size],
                    Offset(-w / 2, -piece.height / 2),
                    Size(w, piece.height),
                    alpha,
                )
            }
        val batch =
            ParticleBatch(
                system,
                painter,
                opacity = physics.opacityCurve(::confettiOpacity),
                growth = physics.growthCurve { 1f },
            )
        batch.origin = Offset(width * origin.pivotFractionX, height * origin.pivotFractionY)
        batch.timeScale = context.speed
        batch.blendMode = physics.blendMode
        context.emit(batch)
    }

    override fun stop() {
        flashJob?.cancel()
    }
}
