package com.jantursky.kapow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.ColorFlashRunner
import com.jantursky.kapow.effects.GlowRunner
import com.jantursky.kapow.effects.PulseRunner
import com.jantursky.kapow.effects.RippleRunner
import com.jantursky.kapow.effects.RiseRunner
import com.jantursky.kapow.effects.ShineRunner
import com.jantursky.kapow.effects.SpinRunner
import com.jantursky.kapow.effects.SprayRunner
import com.jantursky.kapow.internal.ChangeRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * An effect that plays once each time a watched value changes. Attach it with [changeEffect].
 *
 * Effects are compared by value, so creating one inline in a composable is fine.
 */
@Immutable
public abstract class ChangeEffect internal constructor() {
    /** Changes that arrive sooner than this after the last accepted one are ignored. */
    internal abstract val cooldown: Duration

    internal abstract fun createRunner(): ChangeRunner

    /** Plays this effect [delay] after the change instead of right away. */
    public fun delay(delay: Duration): ChangeEffect {
        require(!delay.isNegative()) { "delay must not be negative" }
        return Delayed(this, delay)
    }

    public companion object {
        /**
         * A burst of particles that fan upward from the element, grow, then fade.
         *
         * @param particles what each particle looks like.
         * @param count how many particles a change sprays.
         */
        public fun spray(
            particles: ParticleContent = ParticleContent.shape(),
            count: Int = 11,
        ): ChangeEffect {
            require(count > 0) { "count must be positive" }
            return Spray(particles, count)
        }

        /** One particle per change, such as a "+1", that floats up from the element and fades. */
        public fun rise(particles: ParticleContent = ParticleContent.text("+1")): ChangeEffect = Rise(particles)

        /**
         * A soft band of light that sweeps across the element's drawn pixels.
         *
         * @param angle the direction the band travels, in degrees clockwise from pointing right. 45 sweeps from the
         *   top left to the bottom right.
         */
        public fun shine(
            angle: Float = 45f,
            duration: Duration = 1.seconds,
            color: Color = Color.White,
        ): ChangeEffect {
            require(duration.isPositive()) { "duration must be positive" }
            return Shine(angle, duration, color)
        }

        /**
         * Rings or fills in the element's [shape] that grow outward behind it and fade.
         *
         * @param count how many pulses a change sends, 0.2 seconds apart.
         */
        public fun pulse(
            shape: Shape = RectangleShape,
            color: Color = Color(0xFF6B62F2),
            style: PulseStyle = PulseStyle.Stroke(),
            count: Int = 1,
        ): ChangeEffect {
            require(count > 0) { "count must be positive" }
            return Pulse(shape, color, style, count)
        }

        /**
         * A colored halo in the element's [shape] that flares up and fades. Quick repeated changes make it
         * stronger, up to a limit.
         */
        public fun glow(
            color: Color = Color(0xFF6B62F2),
            radius: Dp = 16.dp,
            shape: Shape = RectangleShape,
        ): ChangeEffect {
            require(radius >= 0.dp) { "radius must not be negative" }
            return Glow(color, radius, shape)
        }

        /** Spins the element one or more full turns and settles where it started. Changes mid-spin add speed. */
        public fun spin(
            axis: SpinAxis = SpinAxis.Y,
            rate: SpinRate = SpinRate.Default,
        ): ChangeEffect = Spin(axis, rate)

        /**
         * An ink ripple that spreads from [origin] until it covers the element, clipped to the element's [shape],
         * and fades out. With reduced motion the shape tints in [color] for a moment instead, without spreading.
         *
         * @param color the ink. Its alpha sets how strong the ripple is.
         * @param shape the element's shape. The ripple never draws outside it.
         * @param origin where the ripple starts, as fractions of the element's width and height.
         * @param duration how long the ripple takes to spread and fade.
         */
        public fun ripple(
            color: Color = Color(0x66FFFFFF),
            shape: Shape = RectangleShape,
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = 600.milliseconds,
        ): ChangeEffect {
            require(duration.isPositive()) { "duration must be positive" }
            return Ripple(color, shape, origin, duration)
        }

        /**
         * The element's own pixels flash in [color] and fade back, like a hit in a game. Transparent parts of the
         * element stay clear. With reduced motion the flash is less than half as strong.
         *
         * @param duration how long the flash takes to rise and fade back.
         */
        public fun flash(
            color: Color = Color.White,
            duration: Duration = 400.milliseconds,
        ): ChangeEffect {
            require(duration.isPositive()) { "duration must be positive" }
            return Flash(color, duration)
        }
    }

    internal data class Delayed(
        val effect: ChangeEffect,
        val delay: Duration,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = effect.cooldown

        // The node unwraps delays and waits itself, so the wrapped effect's runner does the drawing.
        override fun createRunner(): ChangeRunner = effect.createRunner()
    }

    internal data class Spray(
        val particles: ParticleContent,
        val count: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = SprayRunner(particles, count)
    }

    internal data class Rise(
        val particles: ParticleContent,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = RiseRunner(particles)
    }

    internal data class Shine(
        val angle: Float,
        val duration: Duration,
        val color: Color,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = duration / 2

        override fun createRunner(): ChangeRunner = ShineRunner(angle, duration, color)
    }

    internal data class Pulse(
        val shape: Shape,
        val color: Color,
        val style: PulseStyle,
        val count: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = 200.milliseconds * (count - 1)

        override fun createRunner(): ChangeRunner = PulseRunner(shape, color, style, count)
    }

    internal data class Glow(
        val color: Color,
        val radius: Dp,
        val shape: Shape,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = GlowRunner(color, radius, shape)
    }

    internal data class Spin(
        val axis: SpinAxis,
        val rate: SpinRate,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = SpinRunner(axis, rate)
    }

    internal data class Ripple(
        val color: Color,
        val shape: Shape,
        val origin: TransformOrigin,
        val duration: Duration,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = RippleRunner(color, shape, origin, duration)
    }

    internal data class Flash(
        val color: Color,
        val duration: Duration,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = ColorFlashRunner(color, duration)
    }
}

/** Whether a [ChangeEffect.pulse] draws rings or filled shapes. */
@Immutable
public sealed class PulseStyle {
    public data class Stroke(
        val width: Dp = 2.dp,
    ) : PulseStyle()

    public data object Fill : PulseStyle()
}

/** The axis a [ChangeEffect.spin] turns around. Y turns like a coin on a table, Z like a wheel. */
public enum class SpinAxis { X, Y, Z }

/** How fast a [ChangeEffect.spin] starts. */
public enum class SpinRate { Default, Fast }
