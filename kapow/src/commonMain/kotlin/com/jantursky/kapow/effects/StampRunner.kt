package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
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
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.puffPainter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp

/**
 * The stamp at [seconds] after the change: it comes down from [scale] times its size, faster and faster, and lands
 * after [STAMP_DROP] s. The impact squashes it flat and wide, and it settles back with a small rebound by
 * [STAMP_DURATION] s.
 */
internal fun stampPose(
    seconds: Float,
    scale: Float,
): StampRunner.Pose {
    if (seconds < STAMP_DROP) {
        val f = (seconds / STAMP_DROP).coerceIn(0f, 1f)
        return StampRunner.Pose(1f + (scale - 1f) * (1f - f * f), 1f, 1f, 0.35f + 0.65f * f)
    }
    val u = seconds - STAMP_DROP
    if (u >= STAMP_DURATION - STAMP_DROP) return StampRunner.Pose(1f, 1f, 1f, 1f)
    val q = SQUASH * exp(-u / 0.08f) * cos(2 * PI.toFloat() * u / 0.26f)
    return StampRunner.Pose(1f, 1f + q * 0.7f, 1f - q, 1f)
}

internal const val STAMP_DROP = 0.14f
internal const val STAMP_DURATION = 0.56f
private const val SQUASH = 0.1f

/** A small cloud of dust that runs out sideways along the base, to the left (180°) or the right (0°). */
internal fun stampDustEmission(
    angle: Float,
    density: Density,
): Emission =
    with(density) {
        Emission(
            count = 5,
            angle = Spread(angle - 12f, angle + 12f),
            speed = Spread(50.dp.toPx(), 120.dp.toPx()),
            lifetime = Spread(0.45f, 0.75f),
            gravity = -24.dp.toPx(),
            drag = 0.15f,
            scale = Spread(0.5f, 0.9f),
            offsetY = Spread(-3.dp.toPx(), 0f),
        )
    }

/** Drops the element in like a rubber stamp and puffs dust at its base. */
internal class StampRunner(
    private val scale: Float,
    private val dustColor: Color,
) : ChangeRunner() {
    // At 1 the stamp has landed and settled, which is also how the element looks at rest.
    private val time = Animatable(1f)
    private val flash = Flash()
    private var job: Job? = null
    private var seed = 0

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val pose = stampPose(time.value * STAMP_DURATION, scale)
        layer.scaleX = pose.scale * pose.squashX
        layer.scaleY = pose.scale * pose.squashY
        // The squash keeps the base on the ground: scaling around the center would lift it.
        layer.translationY = size.height * (1f - pose.squashY) / 2
        layer.alpha = pose.alpha
        layer.compositingStrategy = flash.compositingStrategy
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        flash.draw(this)
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        if (context.isReducedMotion()) {
            job = context.scope.launch { flash.play() }
            return
        }
        val total = (STAMP_DURATION * 1000).toInt()
        val drop = (STAMP_DROP * 1000).toInt()
        job =
            context.scope.launch {
                time.snapTo(0f)
                time.animateTo(STAMP_DROP / STAMP_DURATION, tween(drop, easing = LinearEasing))
                dust(context)
                time.animateTo(1f, tween(total - drop, easing = LinearEasing))
            }
    }

    private fun dust(context: EffectContext) {
        val painter = puffPainter(dustColor, with(context.density) { 8.dp.toPx() })
        val size = context.size
        for ((side, angle) in listOf(180f, 0f).withIndex()) {
            val batch =
                ParticleBatch(
                    ParticleSystem(stampDustEmission(angle, context.density), seed++),
                    painter,
                    opacity = { 0.75f * (1f - it) },
                    growth = { 0.5f + it },
                )
            batch.origin = Offset(if (side == 0) 0f else size.width.toFloat(), size.height.toFloat())
            context.emit(batch)
        }
    }

    override fun stop() {
        job?.cancel()
    }

    /** How the element is drawn at one moment: its overall scale, the squash on top of it, and its opacity. */
    internal data class Pose(
        val scale: Float,
        val squashX: Float,
        val squashY: Float,
        val alpha: Float,
    )
}
