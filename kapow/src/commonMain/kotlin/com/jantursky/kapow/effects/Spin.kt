package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.SpinAxis
import com.jantursky.kapow.SpinRate
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.time.Duration

/**
 * The angle a spin heads for: [turns] full turns past the current angle, rounded up to a whole turn, and never
 * short of [previousTarget] plus a turn while a spin is still running, so a change mid-spin adds a turn.
 */
internal fun spinTarget(
    current: Float,
    previousTarget: Float?,
    turns: Int,
): Float {
    val fresh = ceil(current / 360f) * 360f + 360f * turns
    return if (previousTarget == null) fresh else maxOf(fresh, previousTarget + 360f)
}

/**
 * How much darker the element is drawn at [degrees] around the X or Y axis: none facing the viewer, most when it
 * is edge-on, so the turn reads as a solid object catching less light.
 */
internal fun spinShade(
    degrees: Float,
    shading: Float = SHADE,
): Float = shading * (1f - abs(cos(degrees * PI.toFloat() / 180f)))

private const val SHADE = 0.35f

/** The spring damping ratio of a spin that overshoots by [overshoot]: the standard 0.65 at the default 0.35. */
internal fun spinDamping(overshoot: Float): Float = (0.65f + (DEFAULT_OVERSHOOT - overshoot)).coerceIn(0.05f, 1f)

internal const val DEFAULT_OVERSHOOT = 0.35f

/** The kick a change gives the spin, in degrees a second. */
internal fun spinImpulse(rate: SpinRate): Float =
    when (rate) {
        SpinRate.Default -> 540f
        SpinRate.Fast -> 1080f
    }

internal class SpinRunner(
    private val axis: SpinAxis,
    private val rate: SpinRate,
    private val turns: Int? = null,
    private val duration: Duration? = null,
    private val easing: Easing = FastOutSlowInEasing,
    private val reverse: Boolean = false,
    private val pivot: TransformOrigin = TransformOrigin.Center,
    private val cameraDistance: Dp = 12.dp,
    private val shading: Float = SHADE,
    private val overshoot: Float = DEFAULT_OVERSHOOT,
) : ChangeRunner() {
    private val angle = Animatable(0f)
    private val flash = Flash()
    private var job: Job? = null
    private var target: Float? = null

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val degrees = if (reverse) -angle.value else angle.value
        if (pivot != TransformOrigin.Center) {
            layer.pivotOffset = Offset(size.width * pivot.pivotFractionX, size.height * pivot.pivotFractionY)
        }
        when (axis) {
            SpinAxis.X -> layer.rotationX = degrees
            SpinAxis.Y -> layer.rotationY = degrees
            SpinAxis.Z -> layer.rotationZ = degrees
        }
        // Keeps the near edge from looming too large while the element turns towards the viewer.
        layer.cameraDistance = cameraDistance.toPx()
        layer.compositingStrategy =
            if (shade() > 0f) CompositingStrategy.Offscreen else flash.compositingStrategy
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val shade = shade()
        if (shade > 0f) drawRect(Color.Black, alpha = shade, blendMode = BlendMode.SrcAtop)
        flash.draw(this)
    }

    // Turning around Z keeps the element facing the viewer, so only X and Y are shaded.
    private fun shade(): Float = if (axis == SpinAxis.Z) 0f else spinShade(angle.value, shading)

    override fun fire(context: EffectContext) {
        if (context.isReducedMotion()) {
            job?.cancel()
            if (context.showsReducedStandIn()) job = context.scope.launch { flash.play(context) }
            return
        }
        val running = angle.isRunning
        val count = turns ?: if (rate == SpinRate.Fast) 2 else 1
        val goal = spinTarget(angle.value, target.takeIf { running }, turns = count)
        val speed = context.speed
        // A faster spring is a stiffer one that starts faster: time shrinks by the same factor.
        val velocity = ((if (running) angle.velocity else 0f) + spinImpulse(rate)) * speed
        target = goal
        job?.cancel()
        job =
            context.scope.launch {
                val length = duration
                if (length == null) {
                    val spec = spring<Float>(dampingRatio = spinDamping(overshoot), stiffness = 10f * speed * speed)
                    angle.animateTo(goal, spec, initialVelocity = velocity)
                } else {
                    angle.animateTo(goal, tween(length.at(speed).inWholeMilliseconds.toInt(), easing = easing))
                }
                // Whole turns look the same as none; starting from 0 keeps the numbers small.
                angle.snapTo(0f)
                target = null
            }
    }

    override fun stop() {
        job?.cancel()
    }
}
