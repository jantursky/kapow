package com.jantursky.kapow

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
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
import com.jantursky.kapow.effects.HeartsRunner
import com.jantursky.kapow.effects.OutlineRoute
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
         * @param easing the curve of the sweep.
         * @param bandWidth the width of the band, as a fraction of the element's length along the sweep.
         * @param softness how soft the band's edges are, above 0 and at most 1. Lower values reach full strength
         *   sooner, so the band looks harder edged.
         * @param peakAlpha how strong the band is at its brightest, as a fraction of [color]'s alpha, from 0 to 1.
         * @param blendMode how the band blends with the element. The default keeps it on the element's own pixels;
         *   a mode such as `BlendMode.Plus` lights them up instead.
         * @param bands how many bands sweep across, one behind the other.
         * @param bandSpacing the distance between bands, in band widths. Used with more than one band.
         * @param reverse sweeps the other way, from the far end of [angle] back towards the start.
         */
        public fun shine(
            angle: Float = 45f,
            duration: Duration = 1.seconds,
            color: Color = Color.White,
            easing: Easing = FastOutSlowInEasing,
            bandWidth: Float = 0.6f,
            softness: Float = 1f,
            peakAlpha: Float = 0.8f,
            blendMode: BlendMode = BlendMode.SrcAtop,
            bands: Int = 1,
            bandSpacing: Float = 1.5f,
            reverse: Boolean = false,
        ): ChangeEffect {
            require(duration.isPositive()) { "duration must be positive" }
            require(bandWidth > 0f && bandWidth.isFinite()) { "bandWidth must be positive" }
            require(softness > 0f && softness <= 1f) { "softness must be above 0 and at most 1" }
            require(peakAlpha in 0f..1f) { "peakAlpha must be between 0 and 1" }
            require(bands > 0) { "bands must be positive" }
            require(bandSpacing >= 0f && bandSpacing.isFinite()) { "bandSpacing must not be negative" }
            return Shine(
                angle,
                duration,
                color,
                easing,
                bandWidth,
                softness,
                peakAlpha,
                blendMode,
                bands,
                bandSpacing,
                reverse,
            )
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun shine(
            angle: Float = 45f,
            duration: Duration = 1.seconds,
            color: Color = Color.White,
        ): ChangeEffect = shine(angle, duration, color, FastOutSlowInEasing)

        /**
         * Rings or fills in the element's [shape] that grow outward behind it and fade.
         *
         * @param count how many pulses a change sends, [interval] apart.
         * @param brush draws the pulses instead of [color], so they can be gradients. Its colors keep their own
         *   alpha.
         * @param reach how far the pulses grow past the element's edge. Unspecified uses three quarters of the
         *   element's shorter side, at most 64 dp.
         * @param duration how long one pulse takes to grow and fade. Null is 1.5 seconds for rings and 2 for fills.
         * @param interval the time between the pulses of one change.
         * @param easing the curve of the pulse's life, from born to gone.
         * @param startInset how far inside the element's edge a pulse starts. The pulse is drawn behind the element,
         *   so an inset one starts hidden.
         * @param fade maps the pulse's life progress to how far it has faded (0 visible, 1 gone). Null fades it
         *   out quickly at first, then slowly.
         */
        public fun pulse(
            shape: Shape = RectangleShape,
            color: Color = Color(0xFF6B62F2),
            style: PulseStyle = PulseStyle.Stroke(),
            count: Int = 1,
            brush: Brush? = null,
            reach: Dp = Dp.Unspecified,
            duration: Duration? = null,
            interval: Duration = 200.milliseconds,
            easing: Easing = LinearEasing,
            startInset: Dp = 0.dp,
            fade: Easing? = null,
        ): ChangeEffect {
            require(count > 0) { "count must be positive" }
            require(reach == Dp.Unspecified || reach >= 0.dp) { "reach must not be negative" }
            require(duration == null || duration.isPositive()) { "duration must be positive" }
            require(!interval.isNegative()) { "interval must not be negative" }
            require(startInset >= 0.dp) { "startInset must not be negative" }
            return Pulse(shape, color, style, count, brush, reach, duration, interval, easing, startInset, fade)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun pulse(
            shape: Shape = RectangleShape,
            color: Color = Color(0xFF6B62F2),
            style: PulseStyle = PulseStyle.Stroke(),
            count: Int = 1,
        ): ChangeEffect = pulse(shape, color, style, count, null)

        /**
         * A colored halo in the element's [shape] that flares up and fades. Quick repeated changes make it
         * stronger, up to a limit.
         *
         * @param intensity how strong the halo is at its brightest, from 0 to 1, as a factor on [color]'s alpha.
         * @param spread how much wider the halo reaches, added to [radius].
         * @param offset moves the halo away from the element, for a glow cast to one side.
         * @param innerGlow how strong a second glow just inside the element's edge is, from 0 (none) to 1.
         * @param flare how long the halo takes to reach full strength. Setting this or [decay] replaces the default
         *   spring, which overshoots and settles on its own, with a rise and a fall that take the times given (the
         *   other one defaults to 150 ms or 1 second).
         * @param decay how long the halo takes to fade away once it peaks. See [flare].
         * @param stackLimit how strong quick repeated changes can make the halo, as a multiple of a single one's
         *   (which is 1). With reduced motion the halo is faint and short and does not stack.
         */
        public fun glow(
            color: Color = Color(0xFF6B62F2),
            radius: Dp = 16.dp,
            shape: Shape = RectangleShape,
            intensity: Float = 1f,
            spread: Dp = 0.dp,
            offset: DpOffset = DpOffset.Zero,
            innerGlow: Float = 0f,
            flare: Duration? = null,
            decay: Duration? = null,
            stackLimit: Float = 1.5f,
        ): ChangeEffect {
            require(radius >= 0.dp) { "radius must not be negative" }
            require(intensity in 0f..1f) { "intensity must be between 0 and 1" }
            require(spread >= 0.dp) { "spread must not be negative" }
            require(innerGlow in 0f..1f) { "innerGlow must be between 0 and 1" }
            require(flare == null || flare.isPositive()) { "flare must be positive" }
            require(decay == null || decay.isPositive()) { "decay must be positive" }
            require(stackLimit > 0f && stackLimit.isFinite()) { "stackLimit must be positive" }
            return Glow(color, radius, shape, intensity, spread, offset, innerGlow, flare, decay, stackLimit)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun glow(
            color: Color = Color(0xFF6B62F2),
            radius: Dp = 16.dp,
            shape: Shape = RectangleShape,
        ): ChangeEffect = glow(color, radius, shape, 1f)

        /**
         * Spins the element one or more full turns and settles where it started. Changes mid-spin add speed. With
         * reduced motion it does not turn; the element brightens briefly instead.
         *
         * @param turns how many full turns a change makes. Null takes one for [SpinRate.Default] and two for
         *   [SpinRate.Fast].
         * @param duration how long the spin takes. Null lets a spring decide, which kicks off fast, overshoots and
         *   settles on its own; a duration replaces it with a plain animation along [easing].
         * @param easing the curve of the spin, used with a [duration].
         * @param reverse spins the other way round.
         * @param pivot the point the element turns around, as fractions of its width and height.
         * @param cameraDistance how far the viewer is from the element, in dp. Closer makes the turn more dramatic
         *   on X and Y; it has no effect on Z.
         * @param shading how much darker the element gets as it turns edge-on, from 0 (not at all) to 1. Z
         *   spins are never shaded.
         * @param overshoot how far the spring swings past its end and back, from 0 (settles smoothly) to 0.95. It
         *   applies when there is no [duration].
         */
        public fun spin(
            axis: SpinAxis = SpinAxis.Y,
            rate: SpinRate = SpinRate.Default,
            turns: Int? = null,
            duration: Duration? = null,
            easing: Easing = FastOutSlowInEasing,
            reverse: Boolean = false,
            pivot: TransformOrigin = TransformOrigin.Center,
            cameraDistance: Dp = 12.dp,
            shading: Float = 0.35f,
            overshoot: Float = 0.35f,
        ): ChangeEffect {
            require(turns == null || turns > 0) { "turns must be positive" }
            require(duration == null || duration.isPositive()) { "duration must be positive" }
            require(cameraDistance > 0.dp) { "cameraDistance must be positive" }
            require(shading in 0f..1f) { "shading must be between 0 and 1" }
            require(overshoot in 0f..0.95f) { "overshoot must be between 0 and 0.95" }
            return Spin(axis, rate, turns, duration, easing, reverse, pivot, cameraDistance, shading, overshoot)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun spin(
            axis: SpinAxis = SpinAxis.Y,
            rate: SpinRate = SpinRate.Default,
        ): ChangeEffect = spin(axis, rate, null)

        /**
         * An ink ripple that spreads from [origin] until it covers the element, clipped to the element's [shape],
         * and fades out. With reduced motion the shape tints in [color] for a moment instead, without spreading.
         *
         * @param color the ink. Its alpha sets how strong the ripple is.
         * @param shape the element's shape. The ripple never draws outside it.
         * @param origin where the ripple starts, as fractions of the element's width and height.
         * @param duration how long the ripple takes to spread and fade.
         * @param rings how many rings spread, one after another.
         * @param ringWidth the width of each ring. Unspecified draws each ring as a filled disc, as ink.
         * @param maxRadius how far the rings spread. Unspecified is as far as the element's farthest corner.
         * @param ringSpacing how long after the one before each ring starts, as a fraction of [duration]. The rings
         *   still end together with the ripple, so (rings - 1) times the spacing must stay below 1.
         * @param easing the curve of the ripple's life.
         */
        public fun ripple(
            color: Color = Color(0x66FFFFFF),
            shape: Shape = RectangleShape,
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = 600.milliseconds,
            rings: Int = 1,
            ringWidth: Dp = Dp.Unspecified,
            maxRadius: Dp = Dp.Unspecified,
            ringSpacing: Float = 0.2f,
            easing: Easing = LinearEasing,
        ): ChangeEffect {
            require(duration.isPositive()) { "duration must be positive" }
            require(rings > 0) { "rings must be positive" }
            require(ringWidth == Dp.Unspecified || ringWidth > 0.dp) { "ringWidth must be positive" }
            require(maxRadius == Dp.Unspecified || maxRadius > 0.dp) { "maxRadius must be positive" }
            require(ringSpacing >= 0f && (rings - 1) * ringSpacing < 1f) {
                "ringSpacing must not be negative and (rings - 1) * ringSpacing must be below 1"
            }
            return Ripple(color, shape, origin, duration, rings, ringWidth, maxRadius, ringSpacing, easing)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun ripple(
            color: Color = Color(0x66FFFFFF),
            shape: Shape = RectangleShape,
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = 600.milliseconds,
        ): ChangeEffect = ripple(color, shape, origin, duration, 1)

        /**
         * The element's own pixels flash in [color] and fade back, like a hit in a game. Transparent parts of the
         * element stay clear. With reduced motion the flash is less than half as strong.
         *
         * @param duration how long the flash takes to rise and fade back.
         * @param peak how strongly the flash covers the element at its brightest, from just above 0 to 1.
         * @param blendMode how the flash blends with the element. The default tints only the element's own
         *   pixels; `BlendMode.Plus` or `BlendMode.Screen` lighten them instead.
         * @param strobes how many times the flash fires within [duration], each a full rise and fade.
         * @param shape limits the flash to this shape within the element. Null flashes the whole element.
         */
        public fun flash(
            color: Color = Color.White,
            duration: Duration = 400.milliseconds,
            peak: Float = 0.85f,
            blendMode: BlendMode = BlendMode.SrcAtop,
            strobes: Int = 1,
            shape: Shape? = null,
        ): ChangeEffect {
            require(duration.isPositive()) { "duration must be positive" }
            require(peak > 0f && peak <= 1f) { "peak must be above 0 and at most 1" }
            require(strobes > 0) { "strobes must be positive" }
            return Flash(color, duration, peak, blendMode, strobes, shape)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun flash(
            color: Color = Color.White,
            duration: Duration = 400.milliseconds,
        ): ChangeEffect = flash(color, duration, 0.85f, BlendMode.SrcAtop)

        /**
         * A spiky comic star burst, in [color] with a lighter core, that pops out from behind the element, overshoots
         * a little and fades. Its tips reach past the element's corners. With reduced motion no burst appears and the
         * element brightens briefly instead.
         *
         * @param spikes how many points the burst has. At least 3.
         * @param sizeScale a factor on how far the burst reaches.
         * @param coreColor the color of the lighter core. Unspecified lightens [color].
         * @param rotation how many degrees the burst turns while it plays.
         * @param overshoot how far past its full size the burst pops, as a fraction of it.
         * @param innerRatio the size of the core, as a fraction of the burst's.
         * @param anchor where the burst is centered, as fractions of the element's width and height.
         * @param inFront draws the burst over the element instead of behind it.
         * @param outline the width of an outline around the burst, 0 for none.
         * @param outlineColor the color of the outline.
         * @param duration how long the burst takes to pop and fade.
         * @param seed the first seed. Each change takes the next one, so the same seed gives the same outlines.
         */
        public fun burst(
            color: Color = Color(0xFFFFB547),
            spikes: Int = 12,
            sizeScale: Float = 1f,
            coreColor: Color = Color.Unspecified,
            rotation: Float = 14f,
            overshoot: Float = 0.15f,
            innerRatio: Float = 0.72f,
            anchor: TransformOrigin = TransformOrigin.Center,
            inFront: Boolean = false,
            outline: Dp = 0.dp,
            outlineColor: Color = Color(0xFF1B1B1F),
            duration: Duration = 600.milliseconds,
            seed: Int = 0,
        ): ChangeEffect {
            require(spikes >= 3) { "spikes must be at least 3" }
            require(sizeScale > 0f && sizeScale.isFinite()) { "sizeScale must be positive" }
            require(overshoot >= 0f && overshoot.isFinite()) { "overshoot must not be negative" }
            require(innerRatio in 0f..1f) { "innerRatio must be between 0 and 1" }
            require(outline >= 0.dp) { "outline must not be negative" }
            require(duration.isPositive()) { "duration must be positive" }
            return Burst(
                color,
                spikes,
                sizeScale,
                coreColor,
                rotation,
                overshoot,
                innerRatio,
                anchor,
                inFront,
                outline,
                outlineColor,
                duration,
                seed,
            )
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun burst(
            color: Color = Color(0xFFFFB547),
            spikes: Int = 12,
        ): ChangeEffect = burst(color, spikes, 1f)

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
         * A large heart pops over the element with an overshoot and fades, while small hearts float up and fade,
         * like the like button of a feed. The hearts are particles, so inside a `KapowParticleLayer` they are not
         * clipped by the element's parents. With reduced motion a single heart fades in and out in place and no
         * small hearts appear.
         *
         * @param color the color of the large heart, and of the small ones unless [smallColors] is set.
         * @param count how many small hearts float up. 0 leaves them out.
         * @param physics how the small hearts launch and move. By default they fan 70 degrees around straight up,
         *   drift upward against a light gravity and fade late. Every part applies; an [EmissionArea] is centered
         *   on [origin]. The large heart ignores it.
         * @param bigHeart whether the large heart pops. Without it only the small hearts float up (with reduced
         *   motion the single heart still shows).
         * @param bigHeartScale a factor on the large heart's size, which is 70 % of the element's shorter side but
         *   at least 28 dp.
         * @param overshoot how far the large heart grows past its size before settling: 0 does not pass it, 1 is
         *   the usual overshoot, more passes it further.
         * @param smallColors the colors of the small hearts, taken in turn. Empty uses [color].
         * @param heartSize the size of a small heart.
         * @param origin where the hearts appear, as fractions of the element's width and height.
         * @param seed the first seed. Each change takes the next one, so the same seed gives the same hearts.
         */
        public fun hearts(
            color: Color = Color(0xFFFF4D6D),
            count: Int = 6,
            physics: ParticlePhysics = ParticlePhysics.Default,
            seed: Int = 0,
            bigHeart: Boolean = true,
            bigHeartScale: Float = 1f,
            overshoot: Float = 1f,
            smallColors: List<Color> = emptyList(),
            heartSize: Dp = 14.dp,
            origin: TransformOrigin = TransformOrigin.Center,
        ): ChangeEffect {
            require(count >= 0) { "count must not be negative" }
            require(bigHeartScale > 0f && bigHeartScale.isFinite()) { "bigHeartScale must be positive" }
            require(overshoot >= 0f && overshoot.isFinite()) { "overshoot must not be negative" }
            require(heartSize > 0.dp) { "heartSize must be positive" }
            return Hearts(
                color,
                count,
                physics,
                seed,
                bigHeart,
                bigHeartScale,
                overshoot,
                smallColors,
                heartSize,
                origin,
            )
        }

        /**
         * A comet-like stroke with a fading tail that runs once around the element's [shape] outline, then
         * disappears. Rectangles and rounded shapes are traced clockwise from the middle of the top edge. The stroke
         * sits just inside the element's bounds. With reduced motion nothing travels: the whole outline shows at
         * once and fades.
         *
         * @param width the stroke's width at the comet's head.
         * @param duration how long the whole run takes, for all [laps].
         * @param tail how long the comet's tail is, as a fraction of the outline, from 0 to 1.
         * @param clockwise whether the comet runs clockwise. Otherwise it runs the other way round.
         * @param startAt where the comet starts, as a fraction of the way round the outline from its usual start
         *   (the middle of the top edge for rectangles).
         * @param laps how many times the comet goes around.
         * @param easing the curve of the run.
         * @param headGlow how strong the faint wide light under the comet's head is, as a factor on the standard.
         *   0 leaves it out.
         * @param brush draws the stroke instead of [color], so the comet can be a gradient.
         */
        public fun trace(
            shape: Shape = RectangleShape,
            color: Color = Color(0xFF6B62F2),
            width: Dp = 3.dp,
            duration: Duration = 1.seconds,
            tail: Float = 0.4f,
            clockwise: Boolean = true,
            startAt: Float = 0f,
            laps: Int = 1,
            easing: Easing = FastOutSlowInEasing,
            headGlow: Float = 1f,
            brush: Brush? = null,
        ): ChangeEffect {
            require(width > 0.dp) { "width must be positive" }
            require(duration.isPositive()) { "duration must be positive" }
            require(tail in 0f..1f) { "tail must be between 0 and 1" }
            require(startAt in 0f..1f) { "startAt must be between 0 and 1" }
            require(laps > 0) { "laps must be positive" }
            require(headGlow >= 0f) { "headGlow must not be negative" }
            return Trace(shape, color, width, duration, tail, clockwise, startAt, laps, easing, headGlow, brush)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun trace(
            shape: Shape = RectangleShape,
            color: Color = Color(0xFF6B62F2),
            width: Dp = 3.dp,
            duration: Duration = 1.seconds,
        ): ChangeEffect = trace(shape, color, width, duration, 0.4f)

        /**
         * A line that draws across the middle of the element, in the reading direction, like crossing off a
         * finished to-do. It stays for [hold], then fades out; keep your own done style, such as dimmed text, for
         * after. With reduced motion the line appears whole instead of drawing across, then holds and fades the
         * same way.
         *
         * @param width the line's thickness.
         * @param direction which way the line runs across the element.
         * @param drawDuration how long the line takes to draw across.
         * @param easing the curve of the drawing.
         * @param offset moves the line across the element, as a fraction of the element's size from the middle, from
         *   -0.5 to 0.5. Positive is lower for horizontal lines and further along for vertical ones.
         * @param cap how the line's ends look. With the default the moving end is rounded while it draws.
         * @param waveAmplitude how far a wavy line swings from straight. 0 draws a straight line.
         * @param waveLength the distance of one wave, for a wavy line.
         * @param lines how many parallel lines are drawn, centered on the offset.
         * @param lineSpacing the distance between parallel lines.
         * @param brush draws the line instead of [color], so it can be a gradient.
         */
        public fun strike(
            color: Color = Color(0xFF6B62F2),
            width: Dp = 2.dp,
            hold: Duration = 600.milliseconds,
            direction: StrikeDirection = StrikeDirection.Reading,
            drawDuration: Duration = 260.milliseconds,
            easing: Easing = FastOutSlowInEasing,
            offset: Float = 0f,
            cap: StrokeCap = StrokeCap.Butt,
            waveAmplitude: Dp = 0.dp,
            waveLength: Dp = 12.dp,
            lines: Int = 1,
            lineSpacing: Dp = 6.dp,
            brush: Brush? = null,
        ): ChangeEffect {
            require(width > 0.dp) { "width must be positive" }
            require(!hold.isNegative()) { "hold must not be negative" }
            require(drawDuration.isPositive()) { "drawDuration must be positive" }
            require(offset in -0.5f..0.5f) { "offset must be between -0.5 and 0.5" }
            require(waveAmplitude >= 0.dp) { "waveAmplitude must not be negative" }
            require(waveLength > 0.dp) { "waveLength must be positive" }
            require(lines > 0) { "lines must be positive" }
            require(lineSpacing >= 0.dp) { "lineSpacing must not be negative" }
            return Strike(
                color,
                width,
                hold,
                direction,
                drawDuration,
                easing,
                offset,
                cap,
                waveAmplitude,
                waveLength,
                lines,
                lineSpacing,
                brush,
            )
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun strike(
            color: Color = Color(0xFF6B62F2),
            width: Dp = 2.dp,
            hold: Duration = 600.milliseconds,
        ): ChangeEffect = strike(color, width, hold, StrikeDirection.Reading)

        /**
         * Brings the element down like a rubber stamp: it drops from [scale] times its size, lands with a squash
         * and a small rebound, and puffs dust at its base. With reduced motion it neither moves nor puffs; it
         * briefly brightens instead.
         *
         * @param scale how much larger the element starts, 1 or more.
         * @param dustColor the color of the dust puffs.
         * @param duration how long the stamp takes from the start of the drop to the end of the rebound. A quarter
         *   of it is the drop.
         * @param squash how flat the impact squashes the element, as a fraction of its size.
         * @param rebound how much the element springs back after the squash, from 0 (settles at once) to 1.
         * @param dustCount how many puffs of dust fly out to each side. 0 leaves out the dust.
         * @param dustSize the size of a puff of dust.
         * @param dustSpread how far the dust flies, as a factor on the standard.
         * @param tilt how many degrees the element is turned as it starts to drop; it levels out as it lands.
         * @param origin the point the element drops around, as fractions of its width and height.
         * @param seed the first seed. Each landing takes the next ones, so the same seed gives the same dust.
         */
        public fun stamp(
            scale: Float = 1.3f,
            dustColor: Color = Color(0xFFB0B0B0),
            duration: Duration = 560.milliseconds,
            squash: Float = 0.1f,
            rebound: Float = 1f,
            dustCount: Int = 5,
            dustSize: Dp = 8.dp,
            dustSpread: Float = 1f,
            tilt: Float = 0f,
            origin: TransformOrigin = TransformOrigin.Center,
            seed: Int = 0,
        ): ChangeEffect {
            require(scale >= 1f) { "scale must be at least 1" }
            require(duration.isPositive()) { "duration must be positive" }
            require(squash in 0f..0.5f) { "squash must be between 0 and 0.5" }
            require(rebound in 0f..1f) { "rebound must be between 0 and 1" }
            require(dustCount >= 0) { "dustCount must not be negative" }
            require(dustSize > 0.dp) { "dustSize must be positive" }
            require(dustSpread >= 0f && dustSpread.isFinite()) { "dustSpread must not be negative" }
            return Stamp(
                scale,
                dustColor,
                duration,
                squash,
                rebound,
                dustCount,
                dustSize,
                dustSpread,
                tilt,
                origin,
                seed,
            )
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun stamp(
            scale: Float = 1.3f,
            dustColor: Color = Color(0xFFB0B0B0),
        ): ChangeEffect = stamp(scale, dustColor, 560.milliseconds)

        /**
         * A digital glitch: for [duration] the element splits into offset red, green and blue copies while
         * horizontal slices of it jump sideways, then it snaps back. With reduced motion nothing shifts; the
         * element briefly brightens instead.
         *
         * @param offset how far the color channels split apart. Slices jump up to one and a half times as far.
         * @param sliceHeight the range a slice's height is picked from, as a fraction of the element's height. Taller
         *   slices mean fewer of them.
         * @param channelColors the colors of the three copies of the element: the first goes one way, the third
         *   the other and the second stays. Each copy keeps one channel of the element, red, green and blue in turn,
         *   tinted in its color, so the default red, green and blue rebuild the original.
         * @param jumpChance the share of slices that jump sideways, from 0 to 1.
         * @param jitterRate how many times a second the glitch changes its look.
         * @param seed the first seed. Each change takes the next one, so the same seed gives the same glitches.
         */
        public fun glitch(
            duration: Duration = 300.milliseconds,
            offset: Dp = 6.dp,
            sliceHeight: ClosedFloatingPointRange<Float> = 0.08f..0.3f,
            channelColors: List<Color> = listOf(Color.Red, Color.Green, Color.Blue),
            jumpChance: Float = 0.5f,
            jitterRate: Float = 20f,
            seed: Int = 0,
        ): ChangeEffect {
            require(duration.isPositive()) { "duration must be positive" }
            require(offset >= 0.dp) { "offset must not be negative" }
            require(sliceHeight.start > 0f && sliceHeight.endInclusive <= 1f) {
                "sliceHeight must be above 0 and at most 1"
            }
            require(channelColors.size == 3) { "channelColors must have three colors" }
            require(jumpChance in 0f..1f) { "jumpChance must be between 0 and 1" }
            require(jitterRate > 0f && jitterRate.isFinite()) { "jitterRate must be positive" }
            return Glitch(duration, offset, sliceHeight, channelColors, jumpChance, jitterRate, seed)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with options.", level = DeprecationLevel.HIDDEN)
        public fun glitch(
            duration: Duration = 300.milliseconds,
            offset: Dp = 6.dp,
        ): ChangeEffect = glitch(duration, offset, 0.08f..0.3f)
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
        val easing: Easing,
        val bandWidth: Float,
        val softness: Float,
        val peakAlpha: Float,
        val blendMode: BlendMode,
        val bands: Int,
        val bandSpacing: Float,
        val reverse: Boolean,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = duration / 2

        override fun createRunner(): ChangeRunner =
            ShineRunner(
                angle,
                duration,
                color,
                easing,
                bandWidth,
                softness,
                peakAlpha,
                blendMode,
                bands,
                bandSpacing,
                reverse,
            )
    }

    internal data class Pulse(
        val shape: Shape,
        val color: Color,
        val style: PulseStyle,
        val count: Int,
        val brush: Brush?,
        val reach: Dp,
        val duration: Duration?,
        val interval: Duration,
        val easing: Easing,
        val startInset: Dp,
        val fade: Easing?,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = interval * (count - 1)

        override fun createRunner(): ChangeRunner =
            PulseRunner(shape, color, style, count, brush, reach, duration, interval, easing, startInset, fade)
    }

    internal data class Glow(
        val color: Color,
        val radius: Dp,
        val shape: Shape,
        val intensity: Float,
        val spread: Dp,
        val offset: DpOffset,
        val innerGlow: Float,
        val flare: Duration?,
        val decay: Duration?,
        val stackLimit: Float,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            GlowRunner(color, radius, shape, intensity, spread, offset, innerGlow, flare, decay, stackLimit)
    }

    internal data class Spin(
        val axis: SpinAxis,
        val rate: SpinRate,
        val turns: Int?,
        val duration: Duration?,
        val easing: Easing,
        val reverse: Boolean,
        val pivot: TransformOrigin,
        val cameraDistance: Dp,
        val shading: Float,
        val overshoot: Float,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            SpinRunner(axis, rate, turns, duration, easing, reverse, pivot, cameraDistance, shading, overshoot)
    }

    internal data class Ripple(
        val color: Color,
        val shape: Shape,
        val origin: TransformOrigin,
        val duration: Duration,
        val rings: Int,
        val ringWidth: Dp,
        val maxRadius: Dp,
        val ringSpacing: Float,
        val easing: Easing,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            RippleRunner(color, shape, origin, duration, rings, ringWidth, maxRadius, ringSpacing, easing)
    }

    internal data class Flash(
        val color: Color,
        val duration: Duration,
        val peak: Float,
        val blendMode: BlendMode,
        val strobes: Int,
        val shape: Shape?,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner = ColorFlashRunner(color, duration, peak, blendMode, strobes, shape)
    }

    internal data class Burst(
        val color: Color,
        val spikes: Int,
        val sizeScale: Float,
        val coreColor: Color,
        val rotation: Float,
        val overshoot: Float,
        val innerRatio: Float,
        val anchor: TransformOrigin,
        val inFront: Boolean,
        val outline: Dp,
        val outlineColor: Color,
        val duration: Duration,
        val seed: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            BurstRunner(
                color,
                spikes,
                sizeScale,
                coreColor,
                rotation,
                overshoot,
                innerRatio,
                anchor,
                inFront,
                outline,
                outlineColor,
                duration,
                seed,
            )
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

    internal data class Hearts(
        val color: Color,
        val count: Int,
        val physics: ParticlePhysics,
        val seed: Int,
        val bigHeart: Boolean,
        val bigHeartScale: Float,
        val overshoot: Float,
        val smallColors: List<Color>,
        val heartSize: Dp,
        val origin: TransformOrigin,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            HeartsRunner(
                color,
                count,
                physics,
                bigHeart,
                bigHeartScale,
                overshoot,
                smallColors,
                heartSize,
                origin,
                seed,
            )
    }

    internal data class Trace(
        val shape: Shape,
        val color: Color,
        val width: Dp,
        val duration: Duration,
        val tail: Float,
        val clockwise: Boolean,
        val startAt: Float,
        val laps: Int,
        val easing: Easing,
        val headGlow: Float,
        val brush: Brush?,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = duration / 2

        override fun createRunner(): ChangeRunner =
            TraceRunner(
                shape,
                color,
                width,
                duration,
                tail,
                OutlineRoute(laps, startAt, clockwise),
                easing,
                headGlow,
                brush,
            )
    }

    internal data class Strike(
        val color: Color,
        val width: Dp,
        val hold: Duration,
        val direction: StrikeDirection,
        val drawDuration: Duration,
        val easing: Easing,
        val offset: Float,
        val cap: StrokeCap,
        val waveAmplitude: Dp,
        val waveLength: Dp,
        val lines: Int,
        val lineSpacing: Dp,
        val brush: Brush?,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            StrikeRunner(
                color,
                width,
                hold,
                direction,
                drawDuration,
                easing,
                offset,
                cap,
                waveAmplitude,
                waveLength,
                lines,
                lineSpacing,
                brush,
            )
    }

    internal data class Stamp(
        val scale: Float,
        val dustColor: Color,
        val duration: Duration,
        val squash: Float,
        val rebound: Float,
        val dustCount: Int,
        val dustSize: Dp,
        val dustSpread: Float,
        val tilt: Float,
        val origin: TransformOrigin,
        val seed: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            StampRunner(
                scale,
                dustColor,
                duration,
                squash,
                rebound,
                dustCount,
                dustSize,
                dustSpread,
                tilt,
                origin,
                seed,
            )
    }

    internal data class Glitch(
        val duration: Duration,
        val offset: Dp,
        val sliceHeight: ClosedFloatingPointRange<Float>,
        val channelColors: List<Color>,
        val jumpChance: Float,
        val jitterRate: Float,
        val seed: Int,
    ) : ChangeEffect() {
        override val cooldown: Duration get() = Duration.ZERO

        override fun createRunner(): ChangeRunner =
            GlitchRunner(
                duration,
                offset,
                sliceHeight.start,
                sliceHeight.endInclusive,
                channelColors,
                jumpChance,
                jitterRate,
                seed,
            )
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

/** Which way the line of [ChangeEffect.strike] runs across the element. */
public enum class StrikeDirection {
    /** From the start edge to the end edge, the way text is read in the layout direction. */
    Reading,

    /** From the end edge to the start edge. */
    Reverse,

    /** From the top edge to the bottom edge, through the middle. */
    Vertical,

    /** From the top corner on the start side to the bottom corner on the end side. */
    Diagonal,
}
