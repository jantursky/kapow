package com.jantursky.kapow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.BorderBeamRunner
import com.jantursky.kapow.effects.BubblesRunner
import com.jantursky.kapow.effects.ConditionalGlowRunner
import com.jantursky.kapow.effects.FlameRunner
import com.jantursky.kapow.effects.GrayscaleRunner
import com.jantursky.kapow.effects.MarchingAntsRunner
import com.jantursky.kapow.effects.OrbitRunner
import com.jantursky.kapow.effects.PushDownRunner
import com.jantursky.kapow.effects.RainbowBorderRunner
import com.jantursky.kapow.effects.RepeatRunner
import com.jantursky.kapow.effects.SmokeRunner
import com.jantursky.kapow.effects.SparklesRunner
import com.jantursky.kapow.effects.SpotlightRunner
import com.jantursky.kapow.effects.TunedConditionalRunner
import com.jantursky.kapow.internal.ConditionalRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
         */
        public fun marchingAnts(
            color: Color = Color.White,
            gapColor: Color = Color.Black,
            width: Dp = 2.dp,
            shape: Shape = RectangleShape,
            dash: Dp = 6.dp,
            gap: Dp = 4.dp,
            speed: Dp = 24.dp,
        ): ConditionalEffect {
            require(width > 0.dp) { "width must be positive" }
            require(dash > 0.dp) { "dash must be positive" }
            require(gap >= 0.dp) { "gap must not be negative" }
            require(speed >= 0.dp) { "speed must not be negative" }
            return MarchingAnts(color, gapColor, width, shape, dash, gap, speed)
        }

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
         */
        public fun rainbowBorder(
            colors: List<Color> = RainbowColors,
            width: Dp = 3.dp,
            shape: Shape = RectangleShape,
            duration: Duration = 3.seconds,
        ): ConditionalEffect {
            require(colors.isNotEmpty()) { "colors must not be empty" }
            require(width > 0.dp) { "width must be positive" }
            require(duration.isPositive()) { "duration must be positive" }
            return RainbowBorder(colors, width, shape, duration)
        }

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
         */
        public fun grayscale(amount: Float = 1f): ConditionalEffect {
            require(amount in 0f..1f) { "amount must be between 0 and 1" }
            return Grayscale(amount)
        }

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
         */
        public fun spotlight(
            color: Color = Color(0x99000000),
            shape: Shape = RectangleShape,
            padding: Dp = 8.dp,
            softness: Dp = 24.dp,
        ): ConditionalEffect {
            require(padding >= 0.dp) { "padding must not be negative" }
            require(softness >= 0.dp) { "softness must not be negative" }
            return Spotlight(color, shape, padding, softness)
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
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = BorderBeamRunner(color, width, shape, length, duration)
    }

    internal data class MarchingAnts(
        val color: Color,
        val gapColor: Color,
        val width: Dp,
        val shape: Shape,
        val dash: Dp,
        val gap: Dp,
        val speed: Dp,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner =
            MarchingAntsRunner(color, gapColor, width, shape, dash, gap, speed)
    }

    internal data class RainbowBorder(
        val colors: List<Color>,
        val width: Dp,
        val shape: Shape,
        val duration: Duration,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = RainbowBorderRunner(colors, width, shape, duration)
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
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = GrayscaleRunner(amount)
    }

    internal data class Spotlight(
        val color: Color,
        val shape: Shape,
        val padding: Dp,
        val softness: Dp,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = SpotlightRunner(color, shape, padding, softness)
    }
}
