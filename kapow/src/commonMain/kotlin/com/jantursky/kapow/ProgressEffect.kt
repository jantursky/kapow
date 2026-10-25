package com.jantursky.kapow

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
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
        /**
         * A band of light whose position across the element follows the progress.
         *
         * @param angle the direction the band sweeps, in degrees clockwise from pointing right.
         * @param bandWidth how wide a band is, as a share of the element's length along the sweep, above 0 and up
         *   to 1.5.
         * @param softness how soft the band's edges are: 1 is the usual falloff, more spreads the glow wider and
         *   less pulls it into a tighter core.
         * @param bands how many bands cross together.
         * @param bandSpacing how far apart the bands are, as a share of the element's length along the sweep.
         * @param blendMode how the bands blend with the element. The default keeps them on its own pixels.
         */
        public fun shine(
            angle: Float = 45f,
            color: Color = Color.White,
            bandWidth: Float = 0.6f,
            softness: Float = 1f,
            bands: Int = 1,
            bandSpacing: Float = 0.3f,
            blendMode: BlendMode = BlendMode.SrcAtop,
        ): ProgressEffect {
            require(bandWidth > 0f && bandWidth <= 1.5f) { "bandWidth must be above 0 and at most 1.5" }
            require(softness > 0f && softness.isFinite()) { "softness must be positive" }
            require(bands > 0) { "bands must be positive" }
            require(bandSpacing >= 0f) { "bandSpacing must not be negative" }
            return Shine(angle, color, bandWidth, softness, bands, bandSpacing, blendMode)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun shine(
            angle: Float = 45f,
            color: Color = Color.White,
        ): ProgressEffect = shine(angle, color, 0.6f)

        /**
         * A halo whose strength follows the progress.
         *
         * @param alphaCurve scales the halo's opacity by this curve at the progress, such as to keep it faint at
         *   first. Null leaves the opacity to the halo's strength.
         * @param colorRamp the halo's colors from progress 0 to 1, blended in between. Null keeps [color].
         */
        public fun glow(
            color: Color = Color(0xFF6B62F2),
            radius: Dp = 16.dp,
            shape: Shape = RectangleShape,
            alphaCurve: Easing? = null,
            colorRamp: List<Color>? = null,
        ): ProgressEffect {
            require(radius >= 0.dp) { "radius must not be negative" }
            require(colorRamp == null || colorRamp.isNotEmpty()) { "colorRamp must not be empty" }
            return Glow(color, radius, shape, alphaCurve, colorRamp)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun glow(
            color: Color = Color(0xFF6B62F2),
            radius: Dp = 16.dp,
            shape: Shape = RectangleShape,
        ): ProgressEffect = glow(color, radius, shape, null)

        /**
         * A circular reveal from [origin] whose radius follows the progress.
         *
         * @param softEdge how wide the soft edge of the circle is. 0 gives a hard edge. The element is fully
         *   shown at 1 whatever the edge. It applies to the round iris, not to a custom [shape].
         * @param shape reveals through this shape, grown from [origin], instead of a circle. Null is a circle.
         * @param invert hides the element inside the iris instead: it is fully shown at 0 and gone at 1.
         */
        public fun iris(
            origin: TransformOrigin = TransformOrigin.Center,
            softEdge: Dp = 0.dp,
            shape: Shape? = null,
            invert: Boolean = false,
        ): ProgressEffect {
            require(softEdge >= 0.dp) { "softEdge must not be negative" }
            return Iris(origin, softEdge, shape, invert)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun iris(origin: TransformOrigin = TransformOrigin.Center): ProgressEffect = iris(origin, 0.dp)

        /**
         * A liquid in [color] that fills the element's own pixels from the bottom up to the progress, with a wave on
         * its surface that rolls as the progress changes. Empty at 0, full at 1. Like every progress effect it is
         * drawn at whatever progress you pass, also with reduced motion.
         *
         * @param color the liquid's tint. Its alpha sets how much of the element shows through.
         * @param wave the height of the surface wave. 0 gives a flat surface.
         * @param direction the way the liquid rises. [FillDirection.Left] and [FillDirection.Right] are absolute,
         *   not relative to the layout direction.
         * @param waves how many waves are across the surface.
         * @param roll how many wave lengths the surface rolls sideways over the whole progress. 0 keeps the waves
         *   still.
         * @param colorRamp the liquid's colors from progress 0 to 1, blended in between. Null keeps [color].
         * @param foam the width of a line that rides the surface. 0 draws none.
         * @param foamColor the color of the foam line.
         */
        public fun fill(
            color: Color = Color(0x993D7BFF),
            wave: Dp = 4.dp,
            direction: FillDirection = FillDirection.Up,
            waves: Float = 1.5f,
            roll: Float = 3f,
            colorRamp: List<Color>? = null,
            foam: Dp = 0.dp,
            foamColor: Color = Color.White,
        ): ProgressEffect {
            require(wave >= 0.dp) { "wave must not be negative" }
            require(waves > 0f && waves.isFinite()) { "waves must be positive" }
            require(roll.isFinite()) { "roll must be a number" }
            require(colorRamp == null || colorRamp.isNotEmpty()) { "colorRamp must not be empty" }
            require(foam >= 0.dp) { "foam must not be negative" }
            return Fill(color, wave, direction, waves, roll, colorRamp, foam, foamColor)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun fill(
            color: Color = Color(0x993D7BFF),
            wave: Dp = 4.dp,
        ): ProgressEffect = fill(color, wave, FillDirection.Up)

        /**
         * The element's [shape] outline drawn as a stroke up to the progress: nothing at 0, the closed outline at 1.
         * Rectangles and rounded shapes are traced clockwise from the middle of the top edge. The stroke sits just
         * inside the element's bounds. Like every progress effect it is drawn at whatever progress you pass, also
         * with reduced motion.
         *
         * @param brush strokes the outline instead of [color], such as a gradient.
         * @param clockwise whether the trace goes round clockwise.
         * @param start where the trace starts, as a share of the way round the outline: 0 is the middle of the top
         *   edge, 0.25 the middle of the right edge.
         * @param tail how much of the outline the stroke covers behind its head, above 0 and up to 1. 1 keeps
         *   everything from the start; less leaves a short stroke that travels.
         * @param cap the ends of the stroke.
         */
        public fun trace(
            shape: Shape = RectangleShape,
            color: Color = Color(0xFF6B62F2),
            width: Dp = 3.dp,
            brush: Brush? = null,
            clockwise: Boolean = true,
            start: Float = 0f,
            tail: Float = 1f,
            cap: StrokeCap = StrokeCap.Round,
        ): ProgressEffect {
            require(width > 0.dp) { "width must be positive" }
            require(start in 0f..1f) { "start must be between 0 and 1" }
            require(tail > 0f && tail <= 1f) { "tail must be above 0 and at most 1" }
            return Trace(shape, color, width, brush, clockwise, start, tail, cap)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun trace(
            shape: Shape = RectangleShape,
            color: Color = Color(0xFF6B62F2),
            width: Dp = 3.dp,
        ): ProgressEffect = trace(shape, color, width, null)
    }

    internal data class Shine(
        val angle: Float,
        val color: Color,
        val bandWidth: Float,
        val softness: Float,
        val bands: Int,
        val bandSpacing: Float,
        val blendMode: BlendMode,
    ) : ProgressEffect() {
        override fun createRunner(): ProgressRunner =
            ShineProgressRunner(angle, color, bandWidth, softness, bands, bandSpacing, blendMode)
    }

    internal data class Glow(
        val color: Color,
        val radius: Dp,
        val shape: Shape,
        val alphaCurve: Easing?,
        val colorRamp: List<Color>?,
    ) : ProgressEffect() {
        override fun createRunner(): ProgressRunner = GlowProgressRunner(color, radius, shape, alphaCurve, colorRamp)
    }

    internal data class Iris(
        val origin: TransformOrigin,
        val softEdge: Dp,
        val shape: Shape?,
        val invert: Boolean,
    ) : ProgressEffect() {
        override fun createRunner(): ProgressRunner = IrisProgressRunner(origin, softEdge, shape, invert)
    }

    internal data class Fill(
        val color: Color,
        val wave: Dp,
        val direction: FillDirection,
        val waves: Float,
        val roll: Float,
        val colorRamp: List<Color>?,
        val foam: Dp,
        val foamColor: Color,
    ) : ProgressEffect() {
        override fun createRunner(): ProgressRunner =
            FillProgressRunner(color, wave, direction, waves, roll, colorRamp, foam, foamColor)
    }

    internal data class Trace(
        val shape: Shape,
        val color: Color,
        val width: Dp,
        val brush: Brush?,
        val clockwise: Boolean,
        val start: Float,
        val tail: Float,
        val cap: StrokeCap,
    ) : ProgressEffect() {
        override fun createRunner(): ProgressRunner =
            TraceProgressRunner(shape, color, width, brush, clockwise, start, tail, cap)
    }
}

/** The way a liquid rises in [ProgressEffect.fill]. */
public enum class FillDirection {
    /** From the bottom edge up. */
    Up,

    /** From the top edge down. */
    Down,

    /** From the left edge to the right, whatever the layout direction. */
    Right,

    /** From the right edge to the left, whatever the layout direction. */
    Left,
}
