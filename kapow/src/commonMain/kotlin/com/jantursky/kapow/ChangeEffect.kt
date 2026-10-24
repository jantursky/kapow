package com.jantursky.kapow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.ConfettiPopRunner
import com.jantursky.kapow.effects.GlitchRunner
import com.jantursky.kapow.effects.GlowRunner
import com.jantursky.kapow.effects.PulseRunner
import com.jantursky.kapow.effects.RiseRunner
import com.jantursky.kapow.effects.ShineRunner
import com.jantursky.kapow.effects.SpinRunner
import com.jantursky.kapow.effects.SprayRunner
import com.jantursky.kapow.effects.StampRunner
import com.jantursky.kapow.effects.StrikeRunner
import com.jantursky.kapow.effects.TraceRunner
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
         * A short pop of confetti: small rectangles and ribbons in [colors] burst up and out of the element,
         * tumble, and fall away under gravity. With reduced motion there is no confetti; the element briefly
         * brightens instead.
         *
         * @param count how many pieces a change throws.
         */
        public fun confettiPop(
            colors: List<Color> = ParticleContent.DefaultColors,
            count: Int = 28,
        ): ChangeEffect {
            require(colors.isNotEmpty()) { "colors must not be empty" }
            require(count > 0) { "count must be positive" }
            return ConfettiPop(colors, count)
        }

        /**
         * A comet-like stroke with a fading tail that runs once around the element's [shape] outline, then
         * disappears. Rectangles and rounded shapes are traced clockwise from the middle of the top edge. The stroke
         * sits just inside the element's bounds. With reduced motion nothing travels: the whole outline shows at
         * once and fades.
         *
         * @param width the stroke's width at the comet's head.
         * @param duration how long one lap takes.
         */
        public fun trace(
            shape: Shape = RectangleShape,
            color: Color = Color(0xFF6B62F2),
            width: Dp = 3.dp,
            duration: Duration = 1.seconds,
        ): ChangeEffect {
            require(width > 0.dp) { "width must be positive" }
            require(duration.isPositive()) { "duration must be positive" }
            return Trace(shape, color, width, duration)
        }

        /**
         * A line that draws across the middle of the element, in the reading direction, like crossing off a
         * finished to-do. It stays for [hold], then fades out; keep your own done style, such as dimmed text, for
         * after. With reduced motion the line appears whole instead of drawing across, then holds and fades the
         * same way.
         *
         * @param width the line's thickness.
         */
        public fun strike(
            color: Color = Color(0xFF6B62F2),
            width: Dp = 2.dp,
            hold: Duration = 600.milliseconds,
        ): ChangeEffect {
            require(width > 0.dp) { "width must be positive" }
            require(!hold.isNegative()) { "hold must not be negative" }
            return Strike(color, width, hold)
        }

        /**
         * Brings the element down like a rubber stamp: it drops from [scale] times its size, lands with a squash
         * and a small rebound, and puffs dust at its base. With reduced motion it neither moves nor puffs; it
         * briefly brightens instead.
         *
         * @param scale how much larger the element starts, 1 or more.
         * @param dustColor the color of the dust puffs.
         */
        public fun stamp(
            scale: Float = 1.3f,
            dustColor: Color = Color(0xFFB0B0B0),
        ): ChangeEffect {
            require(scale >= 1f) { "scale must be at least 1" }
            return Stamp(scale, dustColor)
        }

        /**
         * A digital glitch: for [duration] the element splits into offset red, green and blue copies while
         * horizontal slices of it jump sideways, then it snaps back. With reduced motion nothing shifts; the
         * element briefly brightens instead.
         *
         * @param offset how far the color channels split apart. Slices jump up to one and a half times as far.
         */
        public fun glitch(
            duration: Duration = 300.milliseconds,
            offset: Dp = 6.dp,
        ): ChangeEffect {
            require(duration.isPositive()) { "duration must be positive" }
            require(offset >= 0.dp) { "offset must not be negative" }
            return Glitch(duration, offset)
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

    internal data class ConfettiPop(
        val colors: List<Color>,
        val count: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = ConfettiPopRunner(colors, count)
    }

    internal data class Trace(
        val shape: Shape,
        val color: Color,
        val width: Dp,
        val duration: Duration,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = duration / 2

        override fun createRunner(): ChangeRunner = TraceRunner(shape, color, width, duration)
    }

    internal data class Strike(
        val color: Color,
        val width: Dp,
        val hold: Duration,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = StrikeRunner(color, width, hold)
    }

    internal data class Stamp(
        val scale: Float,
        val dustColor: Color,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = StampRunner(scale, dustColor)
    }

    internal data class Glitch(
        val duration: Duration,
        val offset: Dp,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = GlitchRunner(duration, offset)
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
