package com.jantursky.kapow

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.BorderBeamRunner
import com.jantursky.kapow.effects.BreatheRunner
import com.jantursky.kapow.effects.BubblesRunner
import com.jantursky.kapow.effects.ConditionalGlowRunner
import com.jantursky.kapow.effects.FlameRunner
import com.jantursky.kapow.effects.GrayscaleRunner
import com.jantursky.kapow.effects.MarchingAntsRunner
import com.jantursky.kapow.effects.OrbitRunner
import com.jantursky.kapow.effects.PingRunner
import com.jantursky.kapow.effects.PushDownRunner
import com.jantursky.kapow.effects.RainbowBorderRunner
import com.jantursky.kapow.effects.RepeatRunner
import com.jantursky.kapow.effects.ShimmerRunner
import com.jantursky.kapow.effects.SmokeRunner
import com.jantursky.kapow.effects.SparklesRunner
import com.jantursky.kapow.effects.SpotlightRunner
import com.jantursky.kapow.effects.TunedConditionalRunner
import com.jantursky.kapow.internal.ConditionalRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/** An effect that stays on while a condition is true. Attach it with [conditionalEffect]. */
@Immutable
public abstract class ConditionalEffect internal constructor() {
    internal abstract fun createRunner(): ConditionalRunner

    /** How long the effect takes to fade in when the condition turns true, instead of its own fade. */
    public fun fadeIn(duration: Duration): ConditionalEffect {
        require(!duration.isNegative()) { "duration must not be negative" }
        return tuned().copy(fadeIn = duration)
    }

    /** How long the effect takes to fade out when the condition turns false, instead of its own fade. */
    public fun fadeOut(duration: Duration): ConditionalEffect {
        require(!duration.isNegative()) { "duration must not be negative" }
        return tuned().copy(fadeOut = duration)
    }

    /** Waits [delay] after the condition turns true before the effect starts. */
    public fun startDelay(delay: Duration): ConditionalEffect {
        require(!delay.isNegative()) { "delay must not be negative" }
        return tuned().copy(startDelay = delay)
    }

    /** Runs the effect at [multiplier] times the pace: 2 is twice as fast. */
    public fun speed(multiplier: Float): ConditionalEffect {
        require(multiplier > 0f && multiplier.isFinite()) { "multiplier must be positive" }
        val tuned = tuned()
        return tuned.copy(speed = tuned.speed * multiplier)
    }

    /** Chooses what the effect does under reduced motion, instead of its own fallback. */
    public fun reducedMotion(mode: ReducedMotionMode): ConditionalEffect = tuned().copy(mode = mode)

    private fun tuned(): Tuned = this as? Tuned ?: Tuned(this, null, null, Duration.ZERO, 1f, null)

    internal data class Tuned(
        val effect: ConditionalEffect,
        val fadeIn: Duration?,
        val fadeOut: Duration?,
        val startDelay: Duration,
        val speed: Float,
        val mode: ReducedMotionMode?,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = TunedConditionalRunner(effect.createRunner(), this)
    }

    public companion object {
        /** The shortest interval [repeat] accepts. Shorter ones are raised to it. */
        public val MinRepeatInterval: Duration = (1000.0 / 15).milliseconds

        /**
         * Plays [effect] as soon as the condition turns true (or after [initialDelay]), then every [every] while it
         * stays true.
         *
         * @param every the time between two plays. Shorter than [MinRepeatInterval] is raised to it.
         * @param initialDelay how long to wait after the condition turns true before the first play.
         * @param maxCount how many times to play in all, or null to keep playing while the condition holds.
         * @param jitter how much each wait varies, as a share of [every], from 0 (exactly [every]) to 1.
         * @param seed the start of the random numbers that vary the waits.
         */
        public fun repeat(
            effect: ChangeEffect,
            every: Duration,
            initialDelay: Duration = Duration.ZERO,
            maxCount: Int? = null,
            jitter: Float = 0f,
            seed: Int = 0,
        ): ConditionalEffect {
            require(!initialDelay.isNegative()) { "initialDelay must not be negative" }
            require(maxCount == null || maxCount > 0) { "maxCount must be positive" }
            require(jitter in 0f..1f) { "jitter must be between 0 and 1" }
            return Repeat(effect, every.coerceAtLeast(MinRepeatInterval), initialDelay, maxCount, jitter, seed)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun repeat(
            effect: ChangeEffect,
            every: Duration,
        ): ConditionalEffect = repeat(effect, every, Duration.ZERO)

        /**
         * A steady halo in the element's [shape] that fades in while the condition holds. With reduced motion it
         * appears without a fade and does not breathe.
         *
         * @param strength how strong the halo is, as a share of the halo of one change, from just above 0 to 1.5.
         *   The halo reaches `radius * strength` past the element.
         * @param breathe how much the halo swells and shrinks, as a share of its strength, from 0 (steady) to 1.
         * @param period how long one breath takes.
         * @param brighten how much the element itself brightens at full strength, from 0 to 1.
         */
        public fun glow(
            color: Color = Color(0xFF6B62F2),
            radius: Dp = 16.dp,
            shape: Shape = RectangleShape,
            strength: Float = 0.7f,
            breathe: Float = 0f,
            period: Duration = 2.seconds,
            brighten: Float = 0.25f,
        ): ConditionalEffect {
            require(radius >= 0.dp) { "radius must not be negative" }
            require(strength > 0f && strength <= 1.5f) { "strength must be above 0 and at most 1.5" }
            require(breathe in 0f..1f) { "breathe must be between 0 and 1" }
            require(period.isPositive()) { "period must be positive" }
            require(brighten in 0f..1f) { "brighten must be between 0 and 1" }
            return Glow(color, radius, shape, strength, breathe, period, brighten)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun glow(
            color: Color = Color(0xFF6B62F2),
            radius: Dp = 16.dp,
            shape: Shape = RectangleShape,
        ): ConditionalEffect = glow(color, radius, shape, 0.7f)

        /**
         * Scales the element down slightly, as if pressed, and springs back when the condition ends. With reduced
         * motion the element dims instead (by [dim], or a fifth when that is 0).
         *
         * @param damping the damping ratio of the spring. Below 1 it overshoots.
         * @param stiffness the stiffness of the spring.
         * @param dim how much the element dims while pressed, from 0 (not at all) to 1.
         * @param translation how far the element moves down while pressed.
         * @param origin the point the element shrinks toward.
         */
        public fun pushDown(
            scale: Float = 0.95f,
            damping: Float = 0.6f,
            stiffness: Float = 500f,
            dim: Float = 0f,
            translation: Dp = 0.dp,
            origin: TransformOrigin = TransformOrigin.Center,
        ): ConditionalEffect {
            require(scale > 0f) { "scale must be positive" }
            require(damping > 0f) { "damping must be positive" }
            require(stiffness > 0f) { "stiffness must be positive" }
            require(dim in 0f..1f) { "dim must be between 0 and 1" }
            return PushDown(scale, damping, stiffness, dim, translation, origin)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun pushDown(scale: Float = 0.95f): ConditionalEffect = pushDown(scale, 0.6f)

        /**
         * Soft particles that rise from the top of the element, like smoke.
         *
         * Smoke is made of particles: a [KapowParticleLayer] above the element keeps clipping parents from cutting
         * it off. With reduced motion there are none. [physics] sets the launch cone, speed, lifetime, gravity,
         * drag, size, spin, fade, scale and emission area (`EmissionArea.Edge(...)` moves the source to another
         * edge); its blend mode and color pick are ignored.
         *
         * @param rate how often puffs come, as a factor: 2 is twice as many.
         * @param size the radius of a puff at its start.
         * @param opacity how opaque a puff gets at most, from just above 0 to 1.
         * @param wind a sideways push in dp per second squared. Positive blows toward the right.
         * @param physics how the puffs launch and move.
         * @param seed the start of the random numbers that place and shape the puffs.
         */
        public fun smoke(
            color: Color = Color(0xFF8A8A8A),
            rate: Float = 1f,
            size: Dp = 10.dp,
            opacity: Float = 0.7f,
            wind: Float = 0f,
            physics: ParticlePhysics = ParticlePhysics.Default,
            seed: Int = 0,
        ): ConditionalEffect {
            require(rate > 0f && rate.isFinite()) { "rate must be positive" }
            require(size > 0.dp) { "size must be positive" }
            require(opacity > 0f && opacity <= 1f) { "opacity must be above 0 and at most 1" }
            require(wind.isFinite()) { "wind must be a number" }
            return Smoke(color, rate, size, opacity, wind, physics, seed)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun smoke(color: Color = Color(0xFF8A8A8A)): ConditionalEffect = smoke(color, 1f)

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
         * @param brush strokes the beam and its track instead of [color], such as a gradient. The beam is not
         *   whitened toward its head then.
         * @param trackAlpha how visible the faint track under the beam is, from 0 to 1.
         * @param beams how many beams go round, evenly spaced.
         * @param clockwise whether the beams go round clockwise.
         * @param glow how strong the soft halo around the front of the beam is, as a factor. 0 hides it.
         * @param tailFade how quickly the beam fades toward its tail: higher keeps the tail dark for longer and 0
         *   lights the whole beam evenly.
         */
        public fun borderBeam(
            color: Color = Color(0xFF6B62F2),
            width: Dp = 2.dp,
            shape: Shape = RectangleShape,
            length: Float = 0.25f,
            duration: Duration = 2.seconds,
            brush: Brush? = null,
            trackAlpha: Float = 0.18f,
            beams: Int = 1,
            clockwise: Boolean = true,
            glow: Float = 1f,
            tailFade: Float = 1.5f,
        ): ConditionalEffect {
            require(width > 0.dp) { "width must be positive" }
            require(length > 0f && length <= 1f) { "length must be above 0 and at most 1" }
            require(duration.isPositive()) { "duration must be positive" }
            require(trackAlpha in 0f..1f) { "trackAlpha must be between 0 and 1" }
            require(beams > 0) { "beams must be positive" }
            require(glow >= 0f) { "glow must not be negative" }
            require(tailFade >= 0f) { "tailFade must not be negative" }
            return BorderBeam(
                color,
                width,
                shape,
                length,
                duration,
                brush,
                trackAlpha,
                beams,
                clockwise,
                glow,
                tailFade,
            )
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun borderBeam(
            color: Color = Color(0xFF6B62F2),
            width: Dp = 2.dp,
            shape: Shape = RectangleShape,
            length: Float = 0.25f,
            duration: Duration = 2.seconds,
        ): ConditionalEffect = borderBeam(color, width, shape, length, duration, null)

        /**
         * A dashed outline of [shape] whose dashes keep moving along it, the classic look of a selection. The
         * pattern is stretched a little so the dashes meet without a seam. The stroke sits inside the element's
         * bounds.
         *
         * With reduced motion the dashes stand still.
         *
         * @param color the dashes.
         * @param gapColor the line under the dashes, which shows in the gaps. Transparent for gaps you can see
         *   through.
         * @param dash the length of one dash.
         * @param gap the space between two dashes.
         * @param speed how far the dashes move each second.
         * @param brush strokes the dashes instead of [color], such as a gradient.
         * @param clockwise whether the dashes move clockwise.
         * @param roundCaps whether the dashes end in round caps. The pattern keeps its period, so a dash shorter
         *   than the stroke becomes a dot.
         * @param dashOffset how far along the pattern the dashes start.
         */
        public fun marchingAnts(
            color: Color = Color.White,
            gapColor: Color = Color.Black,
            width: Dp = 2.dp,
            shape: Shape = RectangleShape,
            dash: Dp = 6.dp,
            gap: Dp = 4.dp,
            speed: Dp = 24.dp,
            brush: Brush? = null,
            clockwise: Boolean = true,
            roundCaps: Boolean = false,
            dashOffset: Dp = 0.dp,
        ): ConditionalEffect {
            require(width > 0.dp) { "width must be positive" }
            require(dash > 0.dp) { "dash must be positive" }
            require(gap >= 0.dp) { "gap must not be negative" }
            require(speed >= 0.dp) { "speed must not be negative" }
            return MarchingAnts(
                color,
                gapColor,
                width,
                shape,
                dash,
                gap,
                speed,
                brush,
                clockwise,
                roundCaps,
                dashOffset,
            )
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun marchingAnts(
            color: Color = Color.White,
            gapColor: Color = Color.Black,
            width: Dp = 2.dp,
            shape: Shape = RectangleShape,
            dash: Dp = 6.dp,
            gap: Dp = 4.dp,
            speed: Dp = 24.dp,
        ): ConditionalEffect = marchingAnts(color, gapColor, width, shape, dash, gap, speed, null)

        /** Amber, coral, pink, violet, cobalt and teal: the colors [rainbowBorder] turns through by default. */
        public val RainbowColors: List<Color> =
            listOf(
                Color(0xFFFFB547),
                Color(0xFFFF6B5A),
                Color(0xFFF25CC4),
                Color(0xFF6B62F2),
                Color(0xFF3D7BFF),
                Color(0xFF3DD6C6),
            )

        /**
         * An outline of [shape] stroked with a sweep gradient through [colors] that turns around the element, for
         * something premium or special, such as a Pro plan. The stroke sits inside the element's bounds.
         *
         * With reduced motion the gradient stands still.
         *
         * @param colors the colors around the sweep, in order. It closes back to the first.
         * @param duration how long the gradient takes to turn once.
         * @param clockwise whether the gradient turns clockwise.
         * @param easing shapes each turn. The default turns at a constant rate; an ease-in-out one slows down at
         *   the seam.
         * @param alpha how opaque the border is, above 0 and up to 1.
         * @param glow how far a soft halo of the same gradient reaches past the stroke. 0 draws none.
         */
        public fun rainbowBorder(
            colors: List<Color> = RainbowColors,
            width: Dp = 3.dp,
            shape: Shape = RectangleShape,
            duration: Duration = 3.seconds,
            clockwise: Boolean = true,
            easing: Easing = LinearEasing,
            alpha: Float = 1f,
            glow: Dp = 0.dp,
        ): ConditionalEffect {
            require(colors.isNotEmpty()) { "colors must not be empty" }
            require(width > 0.dp) { "width must be positive" }
            require(duration.isPositive()) { "duration must be positive" }
            require(alpha > 0f && alpha <= 1f) { "alpha must be above 0 and at most 1" }
            require(glow >= 0.dp) { "glow must not be negative" }
            return RainbowBorder(colors, width, shape, duration, clockwise, easing, alpha, glow)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun rainbowBorder(
            colors: List<Color> = RainbowColors,
            width: Dp = 3.dp,
            shape: Shape = RectangleShape,
            duration: Duration = 3.seconds,
        ): ConditionalEffect = rainbowBorder(colors, width, shape, duration, true)

        /**
         * Four-point stars that keep twinkling in and out over the element and just past its edges, a few at a
         * time, for something new or special. Each sparkle stays where it appears; their spots, sizes and colors
         * come from [seed].
         *
         * Sparkles are particles: a [KapowParticleLayer] above the element keeps clipping parents from cutting them
         * off. With reduced motion there are none.
         *
         * @param colors the sparkles' colors, used in turn.
         * @param size the size of the largest sparkle.
         * @param count about how many sparkles are visible at once.
         * @param lifetime how long one sparkle twinkles, in seconds, as a range.
         * @param areaPadding how far past the element's edges a sparkle may appear. Negative keeps them inside.
         * @param shape the shape of a sparkle.
         * @param seed the start of the random numbers that place the sparkles.
         */
        public fun sparkles(
            colors: List<Color> = listOf(Color(0xFFFFF1C2), Color(0xFFFFB547)),
            size: Dp = 18.dp,
            count: Int = 4,
            lifetime: ClosedFloatingPointRange<Float> = 0.8f..1.3f,
            areaPadding: Dp = size / 2,
            shape: ParticleShape = ParticleShape.Sparkle,
            seed: Int = 0,
        ): ConditionalEffect {
            require(colors.isNotEmpty()) { "colors must not be empty" }
            require(size > 0.dp) { "size must be positive" }
            require(count > 0) { "count must be positive" }
            require(lifetime.start > 0f) { "lifetime must be positive" }
            return Sparkles(colors, size, count, lifetime, areaPadding, shape, seed)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun sparkles(
            colors: List<Color> = listOf(Color(0xFFFFF1C2), Color(0xFFFFB547)),
            size: Dp = 18.dp,
            count: Int = 4,
        ): ConditionalEffect = sparkles(colors, size, count, 0.8f..1.3f)

        /**
         * A few glowing particles that circle the element on a tipped ellipse, like a ring around a planet. They
         * pass behind the element smaller and fainter, and in front of it at full size, for something live, such
         * as an avatar while its person is online. They are drawn by the element, outside its bounds.
         *
         * With reduced motion there are no particles.
         *
         * @param colors the particles' colors, used in turn.
         * @param count how many particles circle, spaced evenly.
         * @param size the diameter of a particle in front.
         * @param duration how long one turn takes.
         * @param radius how far the orbit reaches from the center, as a factor of the element's half width.
         * @param tilt how far the orbit is tipped, in degrees clockwise. The default lifts its right end.
         * @param clockwise whether the particles turn clockwise.
         * @param depthScale how big a particle is at the back of the orbit, as a share of its size in front, from
         *   just above 0 to 1.
         * @param content what is drawn for each particle instead of a glowing dot, such as an icon or a shape. Its
         *   own size is used and [size] is ignored. Null draws dots in [colors].
         * @param trail how many fading dots follow each particle. 0 gives none.
         */
        public fun orbit(
            colors: List<Color> = ParticleContent.DefaultColors,
            count: Int = 3,
            size: Dp = 8.dp,
            duration: Duration = 2.5.seconds,
            radius: Float = 1.6f,
            tilt: Float = -17.188734f,
            clockwise: Boolean = true,
            depthScale: Float = 0.55f,
            content: ParticleContent? = null,
            trail: Int = 4,
        ): ConditionalEffect {
            require(colors.isNotEmpty()) { "colors must not be empty" }
            require(count > 0) { "count must be positive" }
            require(size > 0.dp) { "size must be positive" }
            require(duration.isPositive()) { "duration must be positive" }
            require(radius > 0f && radius.isFinite()) { "radius must be positive" }
            require(tilt.isFinite()) { "tilt must be a number" }
            require(depthScale > 0f && depthScale <= 1f) { "depthScale must be above 0 and at most 1" }
            require(trail >= 0) { "trail must not be negative" }
            return Orbit(colors, count, size, duration, radius, tilt, clockwise, depthScale, content, trail)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun orbit(
            colors: List<Color> = ParticleContent.DefaultColors,
            count: Int = 3,
            size: Dp = 8.dp,
            duration: Duration = 2.5.seconds,
        ): ConditionalEffect = orbit(colors, count, size, duration, 1.6f)

        /**
         * Small bubbles that keep rising from the element's top edge, wobbling a little, then pop, for something
         * fizzy, bubbling or underwater.
         *
         * Bubbles are particles: a [KapowParticleLayer] above the element keeps clipping parents from cutting them
         * off. With reduced motion there are none. [physics] sets the launch cone, speed, lifetime (how high they
         * rise), gravity, drag, size, spin, fade, scale and emission area; its blend mode and color pick are
         * ignored.
         *
         * @param size the diameter of the largest bubble.
         * @param rate how often bubbles come, as a factor: 2 is twice as many.
         * @param wobble how wide a bubble sways from side to side.
         * @param pop whether a bubble swells and pops at the end of its life. Without it the bubble just thins out.
         * @param colorVariation how much the bubbles differ in tint, from 0 (all [color]) to 1.
         * @param physics how the bubbles launch and move.
         * @param seed the start of the random numbers that place and shape the bubbles.
         */
        public fun bubbles(
            color: Color = Color(0xFF9ED8FF),
            size: Dp = 10.dp,
            rate: Float = 1f,
            wobble: Dp = 3.dp,
            pop: Boolean = true,
            colorVariation: Float = 0f,
            physics: ParticlePhysics = ParticlePhysics.Default,
            seed: Int = 0,
        ): ConditionalEffect {
            require(size > 0.dp) { "size must be positive" }
            require(rate > 0f && rate.isFinite()) { "rate must be positive" }
            require(wobble >= 0.dp) { "wobble must not be negative" }
            require(colorVariation in 0f..1f) { "colorVariation must be between 0 and 1" }
            return Bubbles(color, size, rate, wobble, pop, colorVariation, physics, seed)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun bubbles(
            color: Color = Color(0xFF9ED8FF),
            size: Dp = 10.dp,
        ): ConditionalEffect = bubbles(color, size, 1f)

        /** Pale yellow, amber, coral and a deep red that fades out: the colors [flame] burns through by default. */
        public val FlameColors: List<Color> =
            listOf(Color(0xFFFFE58A), Color(0xFFFFB547), Color(0xFFFF6B5A), Color(0x00D9363E))

        /**
         * Flames above the element, for something on fire, such as a streak. They are many soft puffs that rise,
         * shrink and change color from the first of [colors] to the last as they burn out, overlapping into
         * flickering tongues. Drawn with radial gradients, without sprites or shaders.
         *
         * Flames are particles: a [KapowParticleLayer] above the element keeps clipping parents from cutting them
         * off. With reduced motion there are none. [physics] sets the launch cone, speed, lifetime, gravity, drag,
         * size, spin, fade, scale and emission area; its blend mode and color pick are ignored.
         *
         * @param colors the colors a puff goes through, from its birth to its end. Fade the last one out.
         * @param height about how high the flames reach away from the element.
         * @param width how wide the base of the flames is. Null takes about a fifth of the edge, at most half the
         *   height.
         * @param intensity how dense the flames are, as a factor: 2 is twice as many puffs.
         * @param wind a sideways push in dp per second squared along the edge. Positive blows toward the right, or
         *   down for a flame on the start or end edge.
         * @param edge the edge the flames burn from. [EdgeSide.Start] and [EdgeSide.End] follow the layout direction.
         * @param flicker how fast the flames sway, as a factor.
         * @param physics how the puffs launch and move.
         * @param seed the start of the random numbers that place and shape the puffs.
         */
        public fun flame(
            colors: List<Color> = FlameColors,
            height: Dp = 40.dp,
            width: Dp? = null,
            intensity: Float = 1f,
            wind: Float = 0f,
            edge: EdgeSide = EdgeSide.Top,
            flicker: Float = 1f,
            physics: ParticlePhysics = ParticlePhysics.Default,
            seed: Int = 0,
        ): ConditionalEffect {
            require(colors.size >= 2) { "colors needs at least two colors" }
            require(height > 0.dp) { "height must be positive" }
            require(width == null || width > 0.dp) { "width must be positive" }
            require(intensity > 0f && intensity.isFinite()) { "intensity must be positive" }
            require(wind.isFinite()) { "wind must be a number" }
            require(flicker >= 0f && flicker.isFinite()) { "flicker must not be negative" }
            return Flame(colors, height, width, intensity, wind, edge, flicker, physics, seed)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun flame(
            colors: List<Color> = FlameColors,
            height: Dp = 40.dp,
        ): ConditionalEffect = flame(colors, height, null)

        /**
         * Drains the color from the element, for a disabled or unavailable look. It fades in and out over 0.3 s.
         * The element is drawn through a color filter in a graphics layer of its own.
         *
         * With reduced motion the color goes and comes back without a fade.
         *
         * @param amount how much color is taken away, from 0 (none) to 1 (fully gray).
         * @param tint a color the remaining colors are multiplied by, such as a brown for a sepia look. Null leaves
         *   the colors as they are.
         * @param dim how much the element darkens, from 0 (not at all) to 1.
         */
        public fun grayscale(
            amount: Float = 1f,
            tint: Color? = null,
            dim: Float = 0f,
        ): ConditionalEffect {
            require(amount in 0f..1f) { "amount must be between 0 and 1" }
            require(dim in 0f..1f) { "dim must be between 0 and 1" }
            return Grayscale(amount, tint, dim)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun grayscale(amount: Float = 1f): ConditionalEffect = grayscale(amount, null)

        /**
         * Dims everything around the element and leaves its [shape] clear, with a soft edge, to point at it, as an
         * onboarding tip does.
         *
         * The scrim is drawn by the element itself, far past its bounds. It covers what is drawn before the element,
         * but anything drawn after it, such as later siblings or later siblings of its parents, stays on top; give
         * the element a higher `zIndex` among its siblings where that matters. A parent that clips its children
         * cuts the scrim off at its own bounds.
         *
         * With reduced motion the dim comes and goes without a fade.
         *
         * @param color the scrim. Its alpha is how dark the surroundings get.
         * @param padding how far past the shape the clear area reaches.
         * @param softness how wide the soft edge between the clear area and the full dim is.
         * @param ring how wide a ring around the edge of the clear area is. 0 draws none.
         * @param ringColor the color of the ring.
         * @param breathe how far the clear area grows and shrinks around [padding]. 0 keeps it still. With reduced
         *   motion it does not breathe.
         * @param period how long one breath takes.
         */
        public fun spotlight(
            color: Color = Color(0x99000000),
            shape: Shape = RectangleShape,
            padding: Dp = 8.dp,
            softness: Dp = 24.dp,
            ring: Dp = 0.dp,
            ringColor: Color = Color.White,
            breathe: Dp = 0.dp,
            period: Duration = 1500.milliseconds,
        ): ConditionalEffect {
            require(padding >= 0.dp) { "padding must not be negative" }
            require(softness >= 0.dp) { "softness must not be negative" }
            require(ring >= 0.dp) { "ring must not be negative" }
            require(breathe >= 0.dp) { "breathe must not be negative" }
            require(period.isPositive()) { "period must be positive" }
            return Spotlight(color, shape, padding, softness, ring, ringColor, breathe, period)
        }

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with more options.", level = DeprecationLevel.HIDDEN)
        public fun spotlight(
            color: Color = Color(0x99000000),
            shape: Shape = RectangleShape,
            padding: Dp = 8.dp,
            softness: Dp = 24.dp,
        ): ConditionalEffect = spotlight(color, shape, padding, softness, 0.dp)

        /**
         * A band of light that sweeps the element's drawn pixels again and again while the condition holds, with a
         * pause between sweeps, as a loading placeholder does. It fades out over 200 ms when the condition turns
         * false (change that with [fadeOut]).
         *
         * With reduced motion the element is lightened evenly while the condition holds, and no band travels.
         *
         * @param color the band. Its alpha is how bright the band gets at its middle.
         * @param angle the direction the band travels, in degrees clockwise from pointing right. 20 sweeps mostly
         *   left to right, tilted slightly downward.
         * @param duration how long one sweep takes.
         * @param bandWidth the width of the band, as a fraction of the element's length along the sweep.
         * @param softness how soft the band's edges are, above 0 and at most 1. Lower values look harder edged.
         * @param pause how long the element rests between two sweeps.
         * @param blendMode how the band blends with the element. The default keeps it on the element's own pixels;
         *   a mode such as `BlendMode.Plus` lights them up instead.
         */
        public fun shimmer(
            color: Color = Color.White.copy(alpha = 0.4f),
            angle: Float = 20f,
            duration: Duration = 1500.milliseconds,
            bandWidth: Float = 0.4f,
            softness: Float = 1f,
            pause: Duration = 400.milliseconds,
            blendMode: BlendMode = BlendMode.SrcAtop,
        ): ConditionalEffect {
            require(duration.isPositive()) { "duration must be positive" }
            require(bandWidth > 0f && bandWidth.isFinite()) { "bandWidth must be positive" }
            require(softness > 0f && softness <= 1f) { "softness must be above 0 and at most 1" }
            require(!pause.isNegative()) { "pause must not be negative" }
            return Shimmer(color, angle, duration, bandWidth, softness, pause, blendMode)
        }

        /**
         * A slow swell: the element grows a little and shrinks back on a smooth loop, and can float up and down, while
         * the condition holds. It eases in when the condition turns true and settles back to rest on a spring when it
         * turns false, with no jump. Use it to say that something is alive, such as a mic that is listening.
         *
         * The scale and float are applied to the element's graphics layer, so nothing is laid out again. With reduced
         * motion the element does not move.
         *
         * @param scale how large the element gets at the fullest breath, as a factor of its size. Must be at least 1.
         * @param float how far the element rises at the fullest breath. 0 keeps it in place.
         * @param duration how long one breath, in and out, takes.
         */
        public fun breathe(
            scale: Float = 1.04f,
            float: Dp = 0.dp,
            duration: Duration = 3.seconds,
        ): ConditionalEffect {
            require(scale >= 1f && scale.isFinite()) { "scale must be at least 1" }
            require(float >= 0.dp) { "float must not be negative" }
            require(duration.isPositive()) { "duration must be positive" }
            return Breathe(scale, float, duration)
        }

        /**
         * Rings that keep growing out of the element's outline and fading, like a radar ping or a live indicator,
         * while the condition holds. The rings follow the element's [shape] and are born evenly, one every
         * `duration / rings`. When the condition turns false no new ring starts and the rings in flight finish
         * (`fadeOut(...)` fades them out instead).
         *
         * The rings are drawn behind the element. With reduced motion one faint ring stays still around the element
         * while the condition holds.
         *
         * @param color the color of the rings.
         * @param shape the shape of the element, which the rings follow.
         * @param rings how many rings are alive at once.
         * @param scale how large a ring is when it is gone, as a factor of the element's size. Must be above 1.
         * @param duration how long a ring takes to grow and fade.
         * @param width the stroke width of the rings.
         */
        public fun ping(
            color: Color = Color(0xFF6B62F2),
            shape: Shape = RectangleShape,
            rings: Int = 2,
            scale: Float = 1.6f,
            duration: Duration = 1600.milliseconds,
            width: Dp = 2.dp,
        ): ConditionalEffect {
            require(rings in 1..MAX_PING_RINGS) { "rings must be between 1 and $MAX_PING_RINGS" }
            require(scale > 1f && scale.isFinite()) { "scale must be above 1" }
            require(duration.isPositive()) { "duration must be positive" }
            require(width > 0.dp) { "width must be positive" }
            return Ping(color, shape, rings, scale, duration, width)
        }

        private const val MAX_PING_RINGS = 8
    }

    internal data class Repeat(
        val effect: ChangeEffect,
        val every: Duration,
        val initialDelay: Duration,
        val maxCount: Int?,
        val jitter: Float,
        val seed: Int,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            RepeatRunner(effect, every, initialDelay, maxCount, jitter, seed)
    }

    internal data class Glow(
        val color: Color,
        val radius: Dp,
        val shape: Shape,
        val strength: Float,
        val breathe: Float,
        val period: Duration,
        val brighten: Float,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            ConditionalGlowRunner(color, radius, shape, strength, breathe, period, brighten)
    }

    internal data class PushDown(
        val scale: Float,
        val damping: Float,
        val stiffness: Float,
        val dim: Float,
        val translation: Dp,
        val origin: TransformOrigin,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            PushDownRunner(scale, damping, stiffness, dim, translation, origin)
    }

    internal data class Smoke(
        val color: Color,
        val rate: Float,
        val size: Dp,
        val opacity: Float,
        val wind: Float,
        val physics: ParticlePhysics,
        val seed: Int,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = SmokeRunner(color, rate, size, opacity, wind, physics, seed)
    }

    internal data class BorderBeam(
        val color: Color,
        val width: Dp,
        val shape: Shape,
        val length: Float,
        val duration: Duration,
        val brush: Brush?,
        val trackAlpha: Float,
        val beams: Int,
        val clockwise: Boolean,
        val glow: Float,
        val tailFade: Float,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            BorderBeamRunner(color, width, shape, length, duration, brush, trackAlpha, beams, clockwise, glow, tailFade)
    }

    internal data class MarchingAnts(
        val color: Color,
        val gapColor: Color,
        val width: Dp,
        val shape: Shape,
        val dash: Dp,
        val gap: Dp,
        val speed: Dp,
        val brush: Brush?,
        val clockwise: Boolean,
        val roundCaps: Boolean,
        val dashOffset: Dp,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            MarchingAntsRunner(color, gapColor, width, shape, dash, gap, speed, brush, clockwise, roundCaps, dashOffset)
    }

    internal data class RainbowBorder(
        val colors: List<Color>,
        val width: Dp,
        val shape: Shape,
        val duration: Duration,
        val clockwise: Boolean,
        val easing: Easing,
        val alpha: Float,
        val glow: Dp,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            RainbowBorderRunner(colors, width, shape, duration, clockwise, easing, alpha, glow)
    }

    internal data class Sparkles(
        val colors: List<Color>,
        val size: Dp,
        val count: Int,
        val lifetime: ClosedFloatingPointRange<Float>,
        val areaPadding: Dp,
        val shape: ParticleShape,
        val seed: Int,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            SparklesRunner(colors, size, count, lifetime, areaPadding, shape, seed)
    }

    internal data class Orbit(
        val colors: List<Color>,
        val count: Int,
        val size: Dp,
        val duration: Duration,
        val radius: Float,
        val tilt: Float,
        val clockwise: Boolean,
        val depthScale: Float,
        val content: ParticleContent?,
        val trail: Int,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            OrbitRunner(colors, count, size, duration, radius, tilt, clockwise, depthScale, content, trail)
    }

    internal data class Bubbles(
        val color: Color,
        val size: Dp,
        val rate: Float,
        val wobble: Dp,
        val pop: Boolean,
        val colorVariation: Float,
        val physics: ParticlePhysics,
        val seed: Int,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            BubblesRunner(color, size, rate, wobble, pop, colorVariation, physics, seed)
    }

    internal data class Flame(
        val colors: List<Color>,
        val height: Dp,
        val width: Dp?,
        val intensity: Float,
        val wind: Float,
        val edge: EdgeSide,
        val flicker: Float,
        val physics: ParticlePhysics,
        val seed: Int,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            FlameRunner(colors, height, width, intensity, wind, edge, flicker, physics, seed)
    }

    internal data class Grayscale(
        val amount: Float,
        val tint: Color?,
        val dim: Float,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = GrayscaleRunner(amount, tint, dim)
    }

    internal data class Spotlight(
        val color: Color,
        val shape: Shape,
        val padding: Dp,
        val softness: Dp,
        val ring: Dp,
        val ringColor: Color,
        val breathe: Dp,
        val period: Duration,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            SpotlightRunner(color, shape, padding, softness, ring, ringColor, breathe, period)
    }

    internal data class Shimmer(
        val color: Color,
        val angle: Float,
        val duration: Duration,
        val bandWidth: Float,
        val softness: Float,
        val pause: Duration,
        val blendMode: BlendMode,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            ShimmerRunner(
                color,
                angle,
                duration.toDouble(DurationUnit.SECONDS),
                pause.toDouble(DurationUnit.SECONDS),
                bandWidth,
                softness,
                blendMode,
            )
    }

    internal data class Breathe(
        val scale: Float,
        val float: Dp,
        val duration: Duration,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            BreatheRunner(scale, float, duration.toDouble(DurationUnit.SECONDS))
    }

    internal data class Ping(
        val color: Color,
        val shape: Shape,
        val rings: Int,
        val scale: Float,
        val duration: Duration,
        val width: Dp,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            PingRunner(color, shape, rings, scale, duration.toDouble(DurationUnit.SECONDS), width)
    }
}
