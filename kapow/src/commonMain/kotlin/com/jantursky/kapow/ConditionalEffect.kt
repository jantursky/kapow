package com.jantursky.kapow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.ConditionalGlowRunner
import com.jantursky.kapow.effects.PushDownRunner
import com.jantursky.kapow.effects.RepeatRunner
import com.jantursky.kapow.effects.SmokeRunner
import com.jantursky.kapow.internal.ConditionalRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** An effect that stays on while a condition is true. Attach it with [conditionalEffect]. */
@Immutable
public abstract class ConditionalEffect internal constructor() {
    internal abstract fun createRunner(): ConditionalRunner

    public companion object {
        /** The shortest interval [repeat] accepts. Shorter ones are raised to it. */
        public val MinRepeatInterval: Duration = (1000.0 / 15).milliseconds

        /**
         * Plays [effect] as soon as the condition turns true, then every [every] while it stays true.
         */
        public fun repeat(
            effect: ChangeEffect,
            every: Duration,
        ): ConditionalEffect = Repeat(effect, every.coerceAtLeast(MinRepeatInterval))

        /** A steady halo in the element's [shape] that fades in while the condition holds. */
        public fun glow(
            color: Color = Color(0xFF6B62F2),
            radius: Dp = 16.dp,
            shape: Shape = RectangleShape,
        ): ConditionalEffect = Glow(color, radius, shape)

        /**
         * Scales the element down slightly, as if pressed, and springs back when the condition ends. With reduced
         * motion the element dims instead.
         */
        public fun pushDown(scale: Float = 0.95f): ConditionalEffect {
            require(scale > 0f) { "scale must be positive" }
            return PushDown(scale)
        }

        /** Soft particles that rise from the top of the element, like smoke. */
        public fun smoke(color: Color = Color(0xFF8A8A8A)): ConditionalEffect = Smoke(color)
    }

    internal data class Repeat(
        val effect: ChangeEffect,
        val every: Duration,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = RepeatRunner(effect, every)
    }

    internal data class Glow(
        val color: Color,
        val radius: Dp,
        val shape: Shape,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = ConditionalGlowRunner(color, radius, shape)
    }

    internal data class PushDown(
        val scale: Float,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = PushDownRunner(scale)
    }

    internal data class Smoke(
        val color: Color,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = SmokeRunner(color)
    }
}
