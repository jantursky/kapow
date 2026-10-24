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
import com.jantursky.kapow.effects.MarchingAntsRunner
import com.jantursky.kapow.effects.OrbitRunner
import com.jantursky.kapow.effects.PushDownRunner
import com.jantursky.kapow.effects.RainbowBorderRunner
import com.jantursky.kapow.effects.RepeatRunner
import com.jantursky.kapow.effects.SmokeRunner
import com.jantursky.kapow.effects.SparklesRunner
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
         * come from a fixed seed.
         *
         * Sparkles are particles: a [KapowParticleLayer] above the element keeps clipping parents from cutting them
         * off. With reduced motion there are none.
         *
         * @param colors the sparkles' colors, used in turn.
         * @param size the size of the largest sparkle.
         * @param count about how many sparkles are visible at once.
         */
        public fun sparkles(
            colors: List<Color> = listOf(Color(0xFFFFF1C2), Color(0xFFFFB547)),
            size: Dp = 18.dp,
            count: Int = 4,
        ): ConditionalEffect {
            require(colors.isNotEmpty()) { "colors must not be empty" }
            require(size > 0.dp) { "size must be positive" }
            require(count > 0) { "count must be positive" }
            return Sparkles(colors, size, count)
        }

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
         */
        public fun orbit(
            colors: List<Color> = ParticleContent.DefaultColors,
            count: Int = 3,
            size: Dp = 8.dp,
            duration: Duration = 2.5.seconds,
        ): ConditionalEffect {
            require(colors.isNotEmpty()) { "colors must not be empty" }
            require(count > 0) { "count must be positive" }
            require(size > 0.dp) { "size must be positive" }
            require(duration.isPositive()) { "duration must be positive" }
            return Orbit(colors, count, size, duration)
        }

        /**
         * Small bubbles that keep rising from the element's top edge, wobbling a little, then pop, for something
         * fizzy, bubbling or underwater.
         *
         * Bubbles are particles: a [KapowParticleLayer] above the element keeps clipping parents from cutting them
         * off. With reduced motion there are none.
         *
         * @param size the diameter of the largest bubble.
         */
        public fun bubbles(
            color: Color = Color(0xFF9ED8FF),
            size: Dp = 10.dp,
        ): ConditionalEffect {
            require(size > 0.dp) { "size must be positive" }
            return Bubbles(color, size)
        }

        /** Pale yellow, amber, coral and a deep red that fades out: the colors [flame] burns through by default. */
        public val FlameColors: List<Color> =
            listOf(Color(0xFFFFE58A), Color(0xFFFFB547), Color(0xFFFF6B5A), Color(0x00D9363E))

        /**
         * Flames above the element, for something on fire, such as a streak. They are many soft puffs that rise,
         * shrink and change color from the first of [colors] to the last as they burn out, overlapping into
         * flickering tongues. Drawn with radial gradients, without sprites or shaders.
         *
         * Flames are particles: a [KapowParticleLayer] above the element keeps clipping parents from cutting them
         * off. With reduced motion there are none.
         *
         * @param colors the colors a puff goes through, from its birth to its end. Fade the last one out.
         * @param height about how high the flames reach above the element.
         */
        public fun flame(
            colors: List<Color> = FlameColors,
            height: Dp = 40.dp,
        ): ConditionalEffect {
            require(colors.size >= 2) { "colors needs at least two colors" }
            require(height > 0.dp) { "height must be positive" }
            return Flame(colors, height)
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
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = SparklesRunner(colors, size, count)
    }

    internal data class Orbit(
        val colors: List<Color>,
        val count: Int,
        val size: Dp,
        val duration: Duration,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = OrbitRunner(colors, count, size, duration)
    }

    internal data class Bubbles(
        val color: Color,
        val size: Dp,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = BubblesRunner(color, size)
    }

    internal data class Flame(
        val colors: List<Color>,
        val height: Dp,
    ) : ConditionalEffect() {
        override fun createRunner(): ConditionalRunner = FlameRunner(colors, height)
    }
}
