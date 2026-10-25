package com.jantursky.kapow

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.FillProgressRunner
import com.jantursky.kapow.effects.GlowProgressRunner
import com.jantursky.kapow.effects.IrisProgressRunner
import com.jantursky.kapow.effects.ShapedProgressRunner
import com.jantursky.kapow.effects.ShineProgressRunner
import com.jantursky.kapow.effects.TraceProgressRunner
import com.jantursky.kapow.internal.ProgressRunner

/**
 * An effect drawn at a progress you control, from 0 to 1, such as a scroll position or a hinge angle. Attach it
 * with [progressEffect].
 */
@Immutable
public abstract class ProgressEffect internal constructor() {
    internal abstract fun createRunner(): ProgressRunner

    /** Passes the progress through [easing] before the effect draws it, such as to start slowly. */
    public fun curve(easing: Easing): ProgressEffect = shaped().copy(curve = easing)

    /**
     * Makes the effect follow the progress with a spring instead of jumping to it, so a progress that changes in
     * steps still moves smoothly. The spring runs in the draw phase and does not recompose.
     */
    public fun smoothed(
        dampingRatio: Float = Spring.DampingRatioNoBouncy,
        stiffness: Float = Spring.StiffnessMedium,
    ): ProgressEffect {
        require(dampingRatio > 0f) { "dampingRatio must be positive" }
        require(stiffness > 0f) { "stiffness must be positive" }
        return shaped().copy(smoothing = dampingRatio to stiffness)
    }

    /** Draws nothing under reduced motion when [mode] is [ReducedMotionMode.Nothing]; other modes change nothing. */
    public fun reducedMotion(mode: ReducedMotionMode): ProgressEffect = shaped().copy(mode = mode)

    private fun shaped(): Shaped = this as? Shaped ?: Shaped(this, null, null, ReducedMotionMode.Default)

    internal data class Shaped(
        val effect: ProgressEffect,
        val curve: Easing?,
        val smoothing: Pair<Float, Float>?,
        val mode: ReducedMotionMode,
    ) : ProgressEffect() {
        override fun createRunner(): ProgressRunner =
            ShapedProgressRunner(effect.createRunner(), curve, smoothing, mode)
    }

    public companion object {
        /** A band of light whose position across the element follows the progress. */
        public fun shine(
            angle: Float = 45f,
            color: Color = Color.White,
        ): ProgressEffect = Shine(angle, color)

        /** A halo whose strength follows the progress. */
        public fun glow(
            color: Color = Color(0xFF6B62F2),
            radius: Dp = 16.dp,
            shape: Shape = RectangleShape,
        ): ProgressEffect {
            require(radius >= 0.dp) { "radius must not be negative" }
            return Glow(color, radius, shape)
        }

        /** A circular reveal from [origin] whose radius follows the progress. */
        public fun iris(origin: TransformOrigin = TransformOrigin.Center): ProgressEffect = Iris(origin)

        /**
         * A liquid in [color] that fills the element's own pixels from the bottom up to the progress, with a wave on
         * its surface that rolls as the progress changes. Empty at 0, full at 1. Like every progress effect it is
         * drawn at whatever progress you pass, also with reduced motion.
         *
         * @param color the liquid's tint. Its alpha sets how much of the element shows through.
         * @param wave the height of the surface wave. 0 gives a flat surface.
         */
        public fun fill(
            color: Color = Color(0x993D7BFF),
            wave: Dp = 4.dp,
        ): ProgressEffect {
            require(wave >= 0.dp) { "wave must not be negative" }
            return Fill(color, wave)
        }

        /**
         * The element's [shape] outline drawn as a stroke up to the progress: nothing at 0, the closed outline at 1.
         * Rectangles and rounded shapes are traced clockwise from the middle of the top edge. The stroke sits just
         * inside the element's bounds. Like every progress effect it is drawn at whatever progress you pass, also
         * with reduced motion.
         */
        public fun trace(
            shape: Shape = RectangleShape,
            color: Color = Color(0xFF6B62F2),
            width: Dp = 3.dp,
        ): ProgressEffect {
            require(width > 0.dp) { "width must be positive" }
            return Trace(shape, color, width)
        }
    }

    internal data class Shine(
        val angle: Float,
        val color: Color,
    ) : ProgressEffect() {
        override fun createRunner(): ProgressRunner = ShineProgressRunner(angle, color)
    }

    internal data class Glow(
        val color: Color,
        val radius: Dp,
        val shape: Shape,
    ) : ProgressEffect() {
        override fun createRunner(): ProgressRunner = GlowProgressRunner(color, radius, shape)
    }

    internal data class Iris(
        val origin: TransformOrigin,
    ) : ProgressEffect() {
        override fun createRunner(): ProgressRunner = IrisProgressRunner(origin)
    }

    internal data class Fill(
        val color: Color,
        val wave: Dp,
    ) : ProgressEffect() {
        override fun createRunner(): ProgressRunner = FillProgressRunner(color, wave)
    }

    internal data class Trace(
        val shape: Shape,
        val color: Color,
        val width: Dp,
    ) : ProgressEffect() {
        override fun createRunner(): ProgressRunner = TraceProgressRunner(shape, color, width)
    }
}
