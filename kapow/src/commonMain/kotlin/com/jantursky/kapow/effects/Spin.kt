package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import com.jantursky.kapow.SpinAxis
import com.jantursky.kapow.SpinRate
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos

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
internal fun spinShade(degrees: Float): Float = SHADE * (1f - abs(cos(degrees * PI.toFloat() / 180f)))

private const val SHADE = 0.35f

/** The kick a change gives the spin, in degrees a second. */
internal fun spinImpulse(rate: SpinRate): Float =
    when (rate) {
        SpinRate.Default -> 540f
        SpinRate.Fast -> 1080f
    }

internal class SpinRunner(
    private val axis: SpinAxis,
    private val rate: SpinRate,
) : ChangeRunner() {
    private val angle = Animatable(0f)
    private val flash = Flash()
    private var job: Job? = null
    private var target: Float? = null

    override val needsLayer: Boolean get() = true

    override fun GraphicsLayerScope.applyLayer() {
        val degrees = angle.value
        when (axis) {
            SpinAxis.X -> rotationX = degrees
            SpinAxis.Y -> rotationY = degrees
            SpinAxis.Z -> rotationZ = degrees
        }
        // Keeps the near edge from looming too large while the element turns towards the viewer.
        cameraDistance = 12f * density
        compositingStrategy =
            if (shade() > 0f) CompositingStrategy.Offscreen else flash.compositingStrategy
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val shade = shade()
        if (shade > 0f) drawRect(Color.Black, alpha = shade, blendMode = BlendMode.SrcAtop)
        flash.draw(this)
    }

    // Turning around Z keeps the element facing the viewer, so only X and Y are shaded.
    private fun shade(): Float = if (axis == SpinAxis.Z) 0f else spinShade(angle.value)

    override fun fire(context: EffectContext) {
        if (context.isReducedMotion()) {
            job?.cancel()
            job = context.scope.launch { flash.play() }
            return
        }
        val running = angle.isRunning
        val goal = spinTarget(angle.value, target.takeIf { running }, turns = if (rate == SpinRate.Fast) 2 else 1)
        val velocity = (if (running) angle.velocity else 0f) + spinImpulse(rate)
        target = goal
        job?.cancel()
        job =
            context.scope.launch {
                angle.animateTo(goal, spring(dampingRatio = 0.7f, stiffness = 40f), initialVelocity = velocity)
                // Whole turns look the same as none; starting from 0 keeps the numbers small.
                angle.snapTo(0f)
                target = null
            }
    }

    override fun stop() {
        job?.cancel()
    }
}
