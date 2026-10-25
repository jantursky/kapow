package com.jantursky.kapow

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.BurstRunner
import com.jantursky.kapow.effects.ColorFlashRunner
import com.jantursky.kapow.effects.ConfettiPopRunner
import com.jantursky.kapow.effects.FireworksRunner
import com.jantursky.kapow.effects.GlitchRunner
import com.jantursky.kapow.effects.GlowRunner
import com.jantursky.kapow.effects.PulseRunner
import com.jantursky.kapow.effects.RippleRunner
import com.jantursky.kapow.effects.RiseRunner
import com.jantursky.kapow.effects.ShineRunner
import com.jantursky.kapow.effects.SparkleRunner
import com.jantursky.kapow.effects.SpeedLinesRunner
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

    /**
     * Ignores changes that arrive sooner than [cooldown] after the last one that played, instead of the effect's
     * own cooldown (which is none for most effects).
     */
    public fun cooldown(cooldown: Duration): ChangeEffect {
        require(!cooldown.isNegative()) { "cooldown must not be negative" }
        return tuned().copy(customCooldown = cooldown)
    }

    /**
     * Plays the effect [count] times in a row for each change, [gap] apart. Changes that arrive while the series
     * runs are ignored. A gap shorter than the effect restarts it each time.
     */
    public fun times(
        count: Int,
        gap: Duration = Duration.ZERO,
    ): ChangeEffect {
        require(count > 0) { "count must be positive" }
        require(!gap.isNegative()) { "gap must not be negative" }
        return tuned().copy(times = count, gap = gap)
    }

    /** Plays the effect at [multiplier] times the pace: 2 takes half as long, 0.5 twice as long. */
    public fun speed(multiplier: Float): ChangeEffect {
        require(multiplier > 0f && multiplier.isFinite()) { "multiplier must be positive" }
        val tuned = tuned()
        return tuned.copy(speed = tuned.speed * multiplier)
    }

    /** Chooses what the effect does under reduced motion, instead of its own fallback. */
    public fun reducedMotion(mode: ReducedMotionMode): ChangeEffect = tuned().copy(mode = mode)

    private fun tuned(): Tuned = this as? Tuned ?: Tuned(this, null, 1, Duration.ZERO, 1f, null)

    public companion object {
        /**
         * A burst of particles that fan upward from the element, grow, then fade. With reduced motion no particles
         * appear and the element brightens briefly instead.
         *
         * @param particles what each particle looks like.
         * @param count how many particles a change sprays.
         * @param physics how the particles launch and move. By default they fan 100 degrees around straight up
         *   from within 6 dp of [origin], are braked by drag and pulled down by gravity. Every part of the physics
         *   is used; an [EmissionArea] is centered on [origin].
         * @param origin where the particles start, as fractions of the element's width and height.
         * @param rotation the range each particle's starting angle is picked from, in degrees.
         * @param seed the first seed. Each change takes the next one, so the same seed gives the same sprays.
         */
        public fun spray(
            particles: ParticleContent = ParticleContent.shape(),
            count: Int = 11,
            physics: ParticlePhysics = ParticlePhysics.Default,
            origin: TransformOrigin = TransformOrigin.Center,
            rotation: ClosedFloatingPointRange<Float> = -30f..30f,
            seed: Int = 0,
        ): ChangeEffect {
            require(count > 0) { "count must be positive" }
            return Spray(particles, count, physics, origin, rotation, seed)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with physics.", level = DeprecationLevel.HIDDEN)
        public fun spray(
            particles: ParticleContent = ParticleContent.shape(),
            count: Int = 11,
        ): ChangeEffect = spray(particles, count, ParticlePhysics.Default, TransformOrigin.Center, -30f..30f, 0)

        /**
         * One particle per change, such as a "+1", that floats up from the element and fades. With reduced motion
         * no particle appears and the element brightens briefly instead.
         *
         * @param distance about how far the particle floats. Unspecified keeps the standard 44 dp.
         * @param duration how long the particle lives.
         * @param drift the heading in degrees away from straight up; positive leans right.
         * @param wobble how far the particle sways from side to side.
         * @param startOffset where the particle starts, from [anchor].
         * @param scaleIn the size the particle is born at, as a fraction of its full size. It grows to full size
         *   over the first 15 % of its life.
         * @param scaleOut the size factor at the end of its life: 1 stays, 0.5 shrinks to half.
         * @param fade maps life progress to how far the particle has faded (0 visible, 1 gone). Null keeps it
         *   opaque for the first half of its life, then fades it out.
         * @param label the text shown for a change, from the value that changed, when [particles] is text. Remember
         *   the lambda, as a new one makes the effect unequal. Ignored for other kinds of particle.
         * @param anchor where the particle starts, as fractions of the element's width and height.
         */
        public fun rise(
            particles: ParticleContent = ParticleContent.text("+1"),
            distance: Dp = Dp.Unspecified,
            duration: Duration = 1200.milliseconds,
            drift: Float = 0f,
            wobble: Dp = 4.dp,
            startOffset: DpOffset = DpOffset.Zero,
            scaleIn: Float = 0.4f,
            scaleOut: Float = 1f,
            fade: Easing? = null,
            label: ((Any?) -> String)? = null,
            anchor: TransformOrigin = TransformOrigin(0.5f, 0f),
        ): ChangeEffect {
            require(distance == Dp.Unspecified || distance > 0.dp) { "distance must be positive" }
            require(duration.isPositive()) { "duration must be positive" }
            require(wobble >= 0.dp) { "wobble must not be negative" }
            require(scaleIn in 0f..1f) { "scaleIn must be between 0 and 1" }
            require(scaleOut >= 0f) { "scaleOut must not be negative" }
            return Rise(
                particles,
                distance,
                duration,
                drift,
                wobble,
                startOffset,
                scaleIn,
                scaleOut,
                fade,
                label,
                anchor,
            )
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun rise(particles: ParticleContent = ParticleContent.text("+1")): ChangeEffect =
            rise(particles, Dp.Unspecified)

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
         * @param peak how strongly the flash covers the element at its brightest, from just above 0 to 1.
         */
        public fun flash(
            color: Color = Color.White,
            duration: Duration = 400.milliseconds,
            peak: Float = 0.85f,
        ): ChangeEffect {
            require(duration.isPositive()) { "duration must be positive" }
            require(peak > 0f && peak <= 1f) { "peak must be above 0 and at most 1" }
            return Flash(color, duration, peak)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with peak.", level = DeprecationLevel.HIDDEN)
        public fun flash(
            color: Color = Color.White,
            duration: Duration = 400.milliseconds,
        ): ChangeEffect = flash(color, duration, 0.85f)

        /**
         * A spiky comic star burst, in [color] with a lighter core, that pops out from behind the element, overshoots
         * a little and fades. Its tips reach past the element's corners. With reduced motion no burst appears and the
         * element brightens briefly instead.
         *
         * @param spikes how many points the burst has. At least 3.
         */
        public fun burst(
            color: Color = Color(0xFFFFB547),
            spikes: Int = 12,
        ): ChangeEffect {
            require(spikes >= 3) { "spikes must be at least 3" }
            return Burst(color, spikes)
        }

        /**
         * Comic action lines that fly outward from all of the element's edges and vanish, as if it had just moved
         * or been hit. With reduced motion no lines appear and the element brightens briefly instead.
         *
         * @param count how many lines go around the element.
         * @param length how far the longest line reaches past its start. Unspecified uses half the element's shorter
         *   side, between 16 and 40 dp.
         * @param thickness the width of the widest line.
         * @param innerRadius the gap between the element's edge and where the lines start.
         * @param taper how much each line narrows towards its outer end, from 0 (a round-ended line of even width)
         *   to 1 (ending in a point).
         * @param directions which way the lines point.
         * @param duration how long the lines take to fly out and vanish.
         * @param easing the curve of the flight.
         * @param seed the first seed. Each change takes the next one, so the same seed gives the same lines.
         */
        public fun speedLines(
            color: Color = Color(0xFF6B62F2),
            count: Int = 12,
            length: Dp = Dp.Unspecified,
            thickness: Dp = 3.dp,
            innerRadius: Dp = 6.dp,
            taper: Float = 0f,
            directions: SpeedLineDirections = SpeedLineDirections.All,
            duration: Duration = 500.milliseconds,
            easing: Easing = LinearEasing,
            seed: Int = 0,
        ): ChangeEffect {
            require(count > 0) { "count must be positive" }
            require(length == Dp.Unspecified || length > 0.dp) { "length must be positive" }
            require(thickness > 0.dp) { "thickness must be positive" }
            require(innerRadius >= 0.dp) { "innerRadius must not be negative" }
            require(taper in 0f..1f) { "taper must be between 0 and 1" }
            require(duration.isPositive()) { "duration must be positive" }
            return SpeedLines(color, count, length, thickness, innerRadius, taper, directions, duration, easing, seed)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun speedLines(
            color: Color = Color(0xFF6B62F2),
            count: Int = 12,
        ): ChangeEffect = speedLines(color, count, Dp.Unspecified)

        /**
         * Four-point stars that twinkle in and out one after another over the element, for about 0.8 seconds. Each
         * star swells, turns a little and dies away where it appeared. With reduced motion no stars appear and the
         * element brightens briefly instead.
         *
         * @param count how many stars a change lights up.
         * @param size how big each star is. Unspecified uses a third of the element's shorter side, between 10 and
         *   22 dp.
         * @param duration how long the whole twinkle takes, from the first star appearing to the last one going out.
         * @param spread how much of the element the stars cover, as a factor on its size around its center. Above 1
         *   the stars land outside the element too.
         * @param spin the range the turning speed of each star is picked from, in degrees per second.
         * @param shape what each star looks like.
         * @param seed the first seed. Each change takes the next one, so the same seed gives the same pattern.
         */
        public fun sparkle(
            color: Color = Color.White,
            count: Int = 6,
            size: Dp = Dp.Unspecified,
            duration: Duration = 800.milliseconds,
            spread: Float = 1f,
            spin: ClosedFloatingPointRange<Float> = -90f..90f,
            shape: ParticleShape = ParticleShape.Sparkle,
            seed: Int = 0,
        ): ChangeEffect {
            require(count > 0) { "count must be positive" }
            require(size == Dp.Unspecified || size > 0.dp) { "size must be positive" }
            require(duration.isPositive()) { "duration must be positive" }
            require(spread > 0f && spread.isFinite()) { "spread must be positive" }
            return Sparkle(color, count, size, duration, spread, spin, shape, seed)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun sparkle(
            color: Color = Color.White,
            count: Int = 6,
        ): ChangeEffect = sparkle(color, count, Dp.Unspecified)

        /**
         * A spark that climbs 100 dp from the element's top edge, then bursts into a ring of sparks that slow down,
         * fall and fade, about 1.6 seconds in all. With reduced motion nothing is launched and the element
         * brightens briefly instead.
         *
         * @param colors the colors of the sparks, taken in turn. The rocket is the first one, lightened.
         * @param count how many sparks the explosion throws.
         * @param rocketHeight how high the rocket climbs before it bursts.
         * @param rocketDuration how long the climb takes.
         * @param burstRadius about how far the sparks fly. Unspecified keeps the standard 100 dp.
         * @param trailLength how long the embers behind the rocket last, as a factor on the standard: 0 leaves no
         *   trail.
         * @param particleSize the size of the rocket and the sparks.
         * @param origin where the rocket launches from, as fractions of the element's width and height.
         * @param physics how the sparks of the explosion move. The rocket and its trail ignore it. Direction and
         *   spread, speed, gravity, drag, lifetime, spin, scales, fade, emission area (centered on the burst),
         *   blend mode and color pick all apply; size does not, use [particleSize].
         * @param seed the first seed. Each change takes the next ones, so the same seed gives the same bursts.
         */
        public fun fireworks(
            colors: List<Color> = ParticleContent.DefaultColors,
            count: Int = 28,
            rocketHeight: Dp = 100.dp,
            rocketDuration: Duration = 450.milliseconds,
            burstRadius: Dp = Dp.Unspecified,
            trailLength: Float = 1f,
            particleSize: Dp = 7.dp,
            origin: TransformOrigin = TransformOrigin(0.5f, 0f),
            physics: ParticlePhysics = ParticlePhysics.Default,
            seed: Int = 0,
        ): ChangeEffect {
            require(colors.isNotEmpty()) { "colors must not be empty" }
            require(count > 0) { "count must be positive" }
            require(rocketHeight > 0.dp) { "rocketHeight must be positive" }
            require(rocketDuration.isPositive()) { "rocketDuration must be positive" }
            require(burstRadius == Dp.Unspecified || burstRadius > 0.dp) { "burstRadius must be positive" }
            require(trailLength >= 0f) { "trailLength must not be negative" }
            require(particleSize > 0.dp) { "particleSize must be positive" }
            return Fireworks(
                colors,
                count,
                rocketHeight,
                rocketDuration,
                burstRadius,
                trailLength,
                particleSize,
                origin,
                physics,
                seed,
            )
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun fireworks(
            colors: List<Color> = ParticleContent.DefaultColors,
            count: Int = 28,
        ): ChangeEffect = fireworks(colors, count, 100.dp)

        /**
         * A short pop of confetti: small rectangles and ribbons in [colors] burst up and out of the element,
         * tumble, and fall away under gravity. With reduced motion there is no confetti; the element briefly
         * brightens instead.
         *
         * @param count how many pieces a change throws.
         * @param physics how the pieces launch and move. All parts apply except size and scales: use [pieceSize].
         *   An [EmissionArea] is centered on [origin]; by default pieces start across the middle half of the
         *   element's width.
         * @param ribbonRatio the share of pieces that are long thin ribbons, from 0 to 1. The standard is a third.
         * @param pieceSize the width of the small rectangles; ribbons and the rest are sized from it.
         * @param origin where the confetti starts, as fractions of the element's width and height.
         * @param seed the first seed. Each change takes the next one, so the same seed gives the same pop.
         */
        public fun confettiPop(
            colors: List<Color> = ParticleContent.DefaultColors,
            count: Int = 28,
            physics: ParticlePhysics = ParticlePhysics.Default,
            ribbonRatio: Float = 1f / 3f,
            pieceSize: Dp = 7.dp,
            origin: TransformOrigin = TransformOrigin.Center,
            seed: Int = 0,
        ): ChangeEffect {
            require(colors.isNotEmpty()) { "colors must not be empty" }
            require(count > 0) { "count must be positive" }
            require(ribbonRatio in 0f..1f) { "ribbonRatio must be between 0 and 1" }
            require(pieceSize > 0.dp) { "pieceSize must be positive" }
            return ConfettiPop(colors, count, physics, ribbonRatio, pieceSize, origin, seed)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with physics.", level = DeprecationLevel.HIDDEN)
        public fun confettiPop(
            colors: List<Color> = ParticleContent.DefaultColors,
            count: Int = 28,
        ): ChangeEffect = confettiPop(colors, count, ParticlePhysics.Default)

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

    internal data class Tuned(
        val effect: ChangeEffect,
        val customCooldown: Duration?,
        val times: Int,
        val gap: Duration,
        val speed: Float,
        val mode: ReducedMotionMode?,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = customCooldown ?: effect.cooldown

        override fun createRunner(): ChangeRunner = effect.createRunner()
    }

    internal data class Spray(
        val particles: ParticleContent,
        val count: Int,
        val physics: ParticlePhysics,
        val origin: TransformOrigin,
        val rotation: ClosedFloatingPointRange<Float>,
        val seed: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = SprayRunner(particles, count, physics, origin, rotation, seed)
    }

    internal data class Rise(
        val particles: ParticleContent,
        val distance: Dp,
        val duration: Duration,
        val drift: Float,
        val wobble: Dp,
        val startOffset: DpOffset,
        val scaleIn: Float,
        val scaleOut: Float,
        val fade: Easing?,
        val label: ((Any?) -> String)?,
        val anchor: TransformOrigin,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            RiseRunner(
                particles,
                distance,
                duration,
                drift,
                wobble,
                startOffset,
                scaleIn,
                scaleOut,
                fade,
                label,
                anchor,
            )
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
        val peak: Float,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = ColorFlashRunner(color, duration, peak)
    }

    internal data class Burst(
        val color: Color,
        val spikes: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = BurstRunner(color, spikes)
    }

    internal data class SpeedLines(
        val color: Color,
        val count: Int,
        val length: Dp,
        val thickness: Dp,
        val innerRadius: Dp,
        val taper: Float,
        val directions: SpeedLineDirections,
        val duration: Duration,
        val easing: Easing,
        val seed: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            SpeedLinesRunner(color, count, length, thickness, innerRadius, taper, directions, duration, easing, seed)
    }

    internal data class Sparkle(
        val color: Color,
        val count: Int,
        val size: Dp,
        val duration: Duration,
        val spread: Float,
        val spin: ClosedFloatingPointRange<Float>,
        val shape: ParticleShape,
        val seed: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            SparkleRunner(color, count, size, duration, spread, spin, shape, seed)
    }

    internal data class Fireworks(
        val colors: List<Color>,
        val count: Int,
        val rocketHeight: Dp,
        val rocketDuration: Duration,
        val burstRadius: Dp,
        val trailLength: Float,
        val particleSize: Dp,
        val origin: TransformOrigin,
        val physics: ParticlePhysics,
        val seed: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            FireworksRunner(
                colors,
                count,
                rocketHeight,
                rocketDuration,
                burstRadius,
                trailLength,
                particleSize,
                origin,
                physics,
                seed,
            )
    }

    internal data class ConfettiPop(
        val colors: List<Color>,
        val count: Int,
        val physics: ParticlePhysics,
        val ribbonRatio: Float,
        val pieceSize: Dp,
        val origin: TransformOrigin,
        val seed: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            ConfettiPopRunner(colors, count, physics, ribbonRatio, pieceSize, origin, seed)
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

/** Which way the lines of [ChangeEffect.speedLines] point. */
public enum class SpeedLineDirections {
    /** All the way around the element. */
    All,

    /** Left and right only, in two fans. */
    Horizontal,

    /** Up and down only, in two fans. */
    Vertical,

    /** Over the top only, in a half circle. */
    Arc,
}
