package com.jantursky.kapow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.GlowProgressRunner
import com.jantursky.kapow.effects.IrisProgressRunner
import com.jantursky.kapow.effects.ShineProgressRunner
import com.jantursky.kapow.internal.ProgressRunner

/**
 * An effect drawn at a progress you control, from 0 to 1, such as a scroll position or a hinge angle. Attach it
 * with [progressEffect].
 */
@Immutable
public abstract class ProgressEffect internal constructor() {
    internal abstract fun createRunner(): ProgressRunner

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
}
