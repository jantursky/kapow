package com.jantursky.kapow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.BorderBeamRunner
import com.jantursky.kapow.effects.ConditionalGlowRunner
import com.jantursky.kapow.effects.PushDownRunner
import com.jantursky.kapow.effects.RepeatRunner
import com.jantursky.kapow.effects.SmokeRunner
import com.jantursky.kapow.internal.ConditionalRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
        ): ConditionalEffect {
            require(radius >= 0.dp) { "radius must not be negative" }
            return Glow(color, radius, shape)
        }

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

        /**
         * A short, bright beam of light that travels around the outline of [shape], on a faint track of the same
         * color, for something live or in progress, such as a recording or a running task. The stroke sits inside
         * the element's bounds, like `Modifier.border`.
         *
         * With reduced motion the beam doesn't travel: the whole outline is lit evenly instead.
         *
         * @param width the stroke width of the beam and its track.
         * @param length how much of the outline the beam covers, from just above 0 to 1.
         * @param duration how long the beam takes to go around once.
         */
        public fun borderBeam(
            color: Color = Color(0xFF6B62F2),
            width: Dp = 2.dp,
            shape: Shape = RectangleShape,
            length: Float = 0.25f,
            duration: Duration = 2.seconds,
        ): ConditionalEffect {
            require(width > 0.dp) { "width must be positive" }
            require(length > 0f && length <= 1f) { "length must be above 0 and at most 1" }
            require(duration.isPositive()) { "duration must be positive" }
            return BorderBeam(color, width, shape, length, duration)
        }
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

    internal data class BorderBeam(
        val color: Color,
        val width: Dp,
        val shape: Shape,
        val length: Float,
        val duration: Duration,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = BorderBeamRunner(color, width, shape, length, duration)
    }
}
