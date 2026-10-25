package com.jantursky.kapow

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The direction of the slats in [KapowEnter.blinds] and [KapowExit.blinds], and of the seam in [KapowEnter.curtain]
 * and [KapowExit.curtain].
 */
public enum class BlindsOrientation { Horizontal, Vertical }

/**
 * How content appears inside an `AnimatedVisibility` or `AnimatedContent`. Apply it with [kapowTransition] or
 * [KapowAnimatedVisibility].
 */
@Immutable
public class KapowEnter internal constructor(
    internal val kind: TransitionKind,
    internal val duration: Duration,
    internal val delay: Duration = Duration.ZERO,
    internal val easing: Easing? = null,
    internal val reducedMotion: ReducedMotionMode = ReducedMotionMode.Default,
) {
    /**
     * This transition, started [delay] after the content becomes visible. The content stays hidden until then and
     * the transition plays over its normal duration afterward. Use [KapowStagger.delay] to cascade the items of a
     * list.
     */
    public fun delay(delay: Duration): KapowEnter {
        require(!delay.isNegative()) { "delay must not be negative" }
        return KapowEnter(kind, duration, delay, easing, reducedMotion)
    }

    /**
     * This transition with its progress shaped by [easing] instead of its own curve. Transitions that run their own
     * motion on a linear clock, such as `anvil`, `pop`, `shatter` and `disintegrate`, stretch their whole timeline
     * with it.
     */
    public fun easing(easing: Easing): KapowEnter = KapowEnter(kind, duration, delay, easing, reducedMotion)

    /**
     * This transition with [mode] as its reduced-motion behavior. [ReducedMotionMode.Default] and
     * [ReducedMotionMode.Fade] fade the content in over the transition's duration. [ReducedMotionMode.Nothing] and
     * [ReducedMotionMode.EndState] show the content at once, without a delay.
     */
    public fun reducedMotion(mode: ReducedMotionMode): KapowEnter = KapowEnter(kind, duration, delay, easing, mode)

    override fun equals(other: Any?): Boolean =
        other is KapowEnter && kind == other.kind && duration == other.duration && delay == other.delay &&
            easing == other.easing && reducedMotion == other.reducedMotion

    override fun hashCode(): Int =
        (((31 * kind.hashCode() + duration.hashCode()) * 31 + delay.hashCode()) * 31 + easing.hashCode()) * 31 +
            reducedMotion.hashCode()

    override fun toString(): String = "KapowEnter($kind, $duration${tuningText(delay, easing, reducedMotion)})"

    public companion object {
        /** No Kapow transition. Content appears with whatever stock transition the caller set. */
        public val None: KapowEnter = KapowEnter(TransitionKind.None, Duration.ZERO)

        /** A circle that grows from [origin] until it covers the content. */
        public fun iris(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter = KapowEnter(TransitionKind.Iris(origin), duration.checked())

        /** A sweep around [origin], clockwise from 12 o'clock, like a clock hand. */
        public fun clock(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter = KapowEnter(TransitionKind.Clock(origin), duration.checked())

        /** A straight edge that moves across the content in the direction of [angle] (degrees, 0 = rightward). */
        public fun wipe(
            angle: Float = 0f,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter = KapowEnter(TransitionKind.Wipe(angle, glare = null), duration.checked())

        /** A [wipe] with a bright streak of [color] running along its edge. */
        public fun glare(
            angle: Float = 45f,
            color: Color = Color.White,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter = KapowEnter(TransitionKind.Wipe(angle, glare = color), duration.checked())

        /** Slats that open like venetian blinds. With [staggered], slats nearer the top (or start) open first. */
        public fun blinds(
            slatWidth: Dp = 10.dp,
            orientation: BlindsOrientation = BlindsOrientation.Horizontal,
            staggered: Boolean = false,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter =
            KapowEnter(TransitionKind.Blinds(slatWidth.coerceAtLeast(5.dp), orientation, staggered), duration.checked())

        /** The content drops in from above, lands with a shake and kicks up dust. */
        public fun anvil(duration: Duration = 1400.milliseconds): KapowEnter =
            KapowEnter(TransitionKind.Anvil, duration.checked())

        /** The content pops in with an overshoot while a ring and a burst of particles of [color] go off behind it. */
        public fun pop(
            color: Color = Color(0xFF6B62F2),
            duration: Duration = 1200.milliseconds,
        ): KapowEnter = KapowEnter(TransitionKind.Pop(color), duration.checked())

        /**
         * The content appears as a grid of small squares that turn on one by one in a random order. The order is
         * seeded, so it is the same every time and on every target. With reduced motion it fades in instead.
         *
         * @param cellSize the side of one square, at least 2 dp. Large elements get bigger squares so a frame never
         *   draws more than a few thousand.
         * @param duration how long the transition takes, 600 ms by default.
         */
        public fun dissolve(
            cellSize: Dp = 12.dp,
            duration: Duration = 600.milliseconds,
        ): KapowEnter = KapowEnter(TransitionKind.Dissolve(cellSize.checkedCell()), duration.checked())

        /**
         * Two halves part from the middle to reveal the content, like a stage curtain. With reduced motion it fades
         * in instead.
         *
         * @param orientation the direction of the seam between the halves, as in [blinds]: with
         *   [BlindsOrientation.Vertical] they slide apart to the left and right, with [BlindsOrientation.Horizontal]
         *   up and down.
         * @param duration how long the transition takes, 450 ms by default.
         */
        public fun curtain(
            orientation: BlindsOrientation = BlindsOrientation.Vertical,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter = KapowEnter(TransitionKind.Curtain(orientation), duration.checked())

        /**
         * Like [iris], but the opening is [shape] instead of a circle: it grows from [origin] until it covers the
         * content. Pointed shapes such as a [StarShape] grow past the content's corners, so their inner corners
         * reach them too. With reduced motion it fades in instead.
         *
         * @param shape the opening, drawn in a square box that grows around [origin]. `CircleShape`,
         *   `RoundedCornerShape` or a [StarShape] all work. The shape should hold its own center. Keep a
         *   `GenericShape` in a `remember` or a top-level value, since a new one is a new transition.
         * @param origin the point the shape grows from, as a fraction of the content's size.
         * @param duration how long the transition takes, 600 ms by default.
         */
        public fun shapeIris(
            shape: Shape = StarShape(),
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = 600.milliseconds,
        ): KapowEnter = KapowEnter(TransitionKind.ShapeIris(shape, origin), duration.checked())

        /**
         * A checkerboard of squares grows over the content in two waves: the squares of one color open from their
         * middles, then the others. With reduced motion it fades in instead.
         *
         * @param cellSize the side of one square of the board, at least 2 dp. Large elements get bigger squares
         *   so a frame never draws more than a few thousand.
         * @param duration how long the transition takes, 600 ms by default.
         */
        public fun checker(
            cellSize: Dp = 24.dp,
            duration: Duration = 600.milliseconds,
        ): KapowEnter = KapowEnter(TransitionKind.Checker(cellSize.checkedCell()), duration.checked())

        /**
         * A [wipe] whose leading edge is torn like paper: an irregular zigzag with a light rim along it, moving
         * across the content in the direction of [angle]. With reduced motion it fades in instead.
         *
         * @param angle the direction the edge moves in, in degrees: 0 is rightward, 90 downward.
         * @param toothSize how far the tear strays off a straight line, at least 2 dp. Its jags come at under
         *   half of that apart. They are irregular, but the same every time.
         * @param duration how long the transition takes, 500 ms by default.
         */
        public fun tear(
            angle: Float = 0f,
            toothSize: Dp = 12.dp,
            duration: Duration = 500.milliseconds,
        ): KapowEnter = KapowEnter(TransitionKind.Tear(angle, toothSize.checkedTooth()), duration.checked())

        /**
         * An organic blob grows from [origin] until it covers the content. Its outline is a smooth curve through a
         * few points that each swell and shrink as it grows, so it wobbles like a drop of liquid. The wobble is
         * seeded, the same every time. With reduced motion it fades in instead.
         *
         * @param origin the point the blob grows from, as a fraction of the content's size.
         * @param duration how long the transition takes, 600 ms by default.
         */
        public fun blob(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = 600.milliseconds,
        ): KapowEnter = KapowEnter(TransitionKind.Blob(origin), duration.checked())
    }
}

/**
 * How content disappears inside an `AnimatedVisibility` or `AnimatedContent`. Apply it with [kapowTransition] or
 * [KapowAnimatedVisibility]. Mask transitions run the reverse of their [KapowEnter] counterpart.
 */
@Immutable
public class KapowExit internal constructor(
    internal val kind: TransitionKind,
    internal val duration: Duration,
    internal val delay: Duration = Duration.ZERO,
    internal val easing: Easing? = null,
    internal val reducedMotion: ReducedMotionMode = ReducedMotionMode.Default,
) {
    /**
     * This transition, started [delay] after the content is asked to go. The content stays fully shown until then
     * and the exit plays over its normal duration afterward. The container keeps the content until the exit has
     * ended. Use [KapowStagger.delay] to cascade the items of a list.
     */
    public fun delay(delay: Duration): KapowExit {
        require(!delay.isNegative()) { "delay must not be negative" }
        return KapowExit(kind, duration, delay, easing, reducedMotion)
    }

    /**
     * This transition with its progress shaped by [easing] instead of its own curve, so an ease-in curve
     * starts the exit slowly. Transitions that run their own motion
     * on a linear clock, such as `poof`, `shatter` and `disintegrate`, stretch their whole timeline with it.
     */
    public fun easing(easing: Easing): KapowExit = KapowExit(kind, duration, delay, easing, reducedMotion)

    /**
     * This transition with [mode] as its reduced-motion behavior. [ReducedMotionMode.Default] and
     * [ReducedMotionMode.Fade] fade the content out over the transition's duration. [ReducedMotionMode.Nothing] and
     * [ReducedMotionMode.EndState] remove the content at once, without a delay.
     */
    public fun reducedMotion(mode: ReducedMotionMode): KapowExit = KapowExit(kind, duration, delay, easing, mode)

    override fun equals(other: Any?): Boolean =
        other is KapowExit && kind == other.kind && duration == other.duration && delay == other.delay &&
            easing == other.easing && reducedMotion == other.reducedMotion

    override fun hashCode(): Int =
        (((31 * kind.hashCode() + duration.hashCode()) * 31 + delay.hashCode()) * 31 + easing.hashCode()) * 31 +
            reducedMotion.hashCode()

    override fun toString(): String = "KapowExit($kind, $duration${tuningText(delay, easing, reducedMotion)})"

    public companion object {
        /** No Kapow transition. Content disappears with whatever stock transition the caller set. */
        public val None: KapowExit = KapowExit(TransitionKind.None, Duration.ZERO)

        /** A circle that shrinks toward [origin]. */
        public fun iris(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit = KapowExit(TransitionKind.Iris(origin), duration.checked())

        /** A sweep around [origin] that takes the content away, counterclockwise back to 12 o'clock. */
        public fun clock(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit = KapowExit(TransitionKind.Clock(origin), duration.checked())

        /** A straight edge that covers the content, moving against [angle]. */
        public fun wipe(
            angle: Float = 0f,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit = KapowExit(TransitionKind.Wipe(angle, glare = null), duration.checked())

        /** A [wipe] with a bright streak of [color] along its edge. */
        public fun glare(
            angle: Float = 45f,
            color: Color = Color.White,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit = KapowExit(TransitionKind.Wipe(angle, glare = color), duration.checked())

        /** Slats that close like venetian blinds. */
        public fun blinds(
            slatWidth: Dp = 10.dp,
            orientation: BlindsOrientation = BlindsOrientation.Horizontal,
            staggered: Boolean = false,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit =
            KapowExit(TransitionKind.Blinds(slatWidth.coerceAtLeast(5.dp), orientation, staggered), duration.checked())

        /** The content vanishes at once in a puff of smoke of [color]. */
        public fun poof(
            color: Color = Color(0xFFC2C2C2),
            duration: Duration = 400.milliseconds,
        ): KapowExit = KapowExit(TransitionKind.Poof(color), duration.checked())

        /**
         * The content vanishes as a grid of small squares that turn off one by one, in the reverse of
         * [KapowEnter.dissolve]'s order. With reduced motion it fades out instead.
         *
         * @param cellSize the side of one square, at least 2 dp.
         * @param duration how long the transition takes, 600 ms by default.
         */
        public fun dissolve(
            cellSize: Dp = 12.dp,
            duration: Duration = 600.milliseconds,
        ): KapowExit = KapowExit(TransitionKind.Dissolve(cellSize.checkedCell()), duration.checked())

        /**
         * Two halves close in from the sides and meet in the middle, hiding the content. With reduced motion it fades
         * out instead.
         *
         * @param orientation the direction of the seam where the halves meet, as in [KapowEnter.curtain].
         * @param duration how long the transition takes, 450 ms by default.
         */
        public fun curtain(
            orientation: BlindsOrientation = BlindsOrientation.Vertical,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit = KapowExit(TransitionKind.Curtain(orientation), duration.checked())

        /**
         * Like [iris], but the opening is [shape]: it shrinks toward [origin] until nothing is left. With reduced
         * motion it fades out instead.
         *
         * @param shape the opening, as in [KapowEnter.shapeIris].
         * @param origin the point the shape shrinks toward, as a fraction of the content's size.
         * @param duration how long the transition takes, 600 ms by default.
         */
        public fun shapeIris(
            shape: Shape = StarShape(),
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = 600.milliseconds,
        ): KapowExit = KapowExit(TransitionKind.ShapeIris(shape, origin), duration.checked())

        /**
         * A checkerboard of squares shrinks away in two waves, the reverse of [KapowEnter.checker]. With reduced
         * motion it fades out instead.
         *
         * @param cellSize the side of one square of the board, at least 2 dp.
         * @param duration how long the transition takes, 600 ms by default.
         */
        public fun checker(
            cellSize: Dp = 24.dp,
            duration: Duration = 600.milliseconds,
        ): KapowExit = KapowExit(TransitionKind.Checker(cellSize.checkedCell()), duration.checked())

        /**
         * A torn edge, like [KapowEnter.tear], that covers the content moving against [angle]. With reduced motion
         * it fades out instead.
         *
         * @param angle the direction the edge moved in on enter, in degrees; the exit runs it backward.
         * @param toothSize how far the tear strays off a straight line, at least 2 dp.
         * @param duration how long the transition takes, 500 ms by default.
         */
        public fun tear(
            angle: Float = 0f,
            toothSize: Dp = 12.dp,
            duration: Duration = 500.milliseconds,
        ): KapowExit = KapowExit(TransitionKind.Tear(angle, toothSize.checkedTooth()), duration.checked())

        /**
         * A wobbling blob, like [KapowEnter.blob], that shrinks toward [origin] until nothing is left. With reduced
         * motion it fades out instead.
         *
         * @param origin the point the blob shrinks toward, as a fraction of the content's size.
         * @param duration how long the transition takes, 600 ms by default.
         */
        public fun blob(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = 600.milliseconds,
        ): KapowExit = KapowExit(TransitionKind.Blob(origin), duration.checked())

        /**
         * The content breaks like glass: it cracks into triangular shards that part, then fall with gravity,
         * turning and fading as they go. The shards carry the content's own pixels and may fall below its bounds.
         * The pattern is seeded, the same every time. With reduced motion it fades out instead.
         *
         * @param shards about how many pieces, from 2 to 200. The content is cut into a grid of cells that fits its
         *   shape, two triangles per cell, so the actual count can differ a little.
         * @param duration how long the transition takes, 900 ms by default.
         */
        public fun shatter(
            shards: Int = 24,
            duration: Duration = 900.milliseconds,
        ): KapowExit {
            require(shards in 2..200) { "shards must be from 2 to 200" }
            return KapowExit(TransitionKind.Shatter(shards), duration.checked())
        }

        /**
         * The content turns to dust: a ragged band sweeps across it from left to right, grain by grain, and as it
         * passes, small square particles drift up and away. The band crosses in the first half or so of
         * [duration] and the dust settles in the rest, so a particle layer is not needed for it to finish. With
         * reduced motion it fades out instead, with no particles.
         *
         * @param color the color of the dust. Left unspecified, the dust takes the content's own colors, read once
         *   from a snapshot of the content as the exit starts: a few pixels per column. Where that snapshot can't
         *   be read, or reads only transparent pixels, the dust is light gray.
         * @param duration how long the transition takes, 1500 ms by default.
         */
        public fun disintegrate(
            color: Color = Color.Unspecified,
            duration: Duration = 1500.milliseconds,
        ): KapowExit = KapowExit(TransitionKind.Disintegrate(color, duration.checked()), duration)
    }
}

private val DefaultMaskDuration: Duration = 450.milliseconds

/** The `, delay=…` parts of a transition's text that differ from the defaults, so a plain one reads as before. */
private fun tuningText(
    delay: Duration,
    easing: Easing?,
    reducedMotion: ReducedMotionMode,
): String =
    buildString {
        if (delay.isPositive()) append(", delay=").append(delay)
        if (easing != null) append(", easing=").append(easing)
        if (reducedMotion != ReducedMotionMode.Default) append(", reducedMotion=").append(reducedMotion)
    }

private fun Duration.checked(): Duration {
    require(isPositive()) { "duration must be positive" }
    return this
}

private fun Dp.checkedCell(): Dp {
    require(this >= 2.dp) { "cellSize must be at least 2 dp" }
    return this
}

private fun Dp.checkedTooth(): Dp {
    require(this >= 2.dp) { "toothSize must be at least 2 dp" }
    return this
}

internal sealed class TransitionKind {
    data object None : TransitionKind()

    data class Iris(
        val origin: TransformOrigin,
    ) : TransitionKind()

    data class Clock(
        val origin: TransformOrigin,
    ) : TransitionKind()

    data class Wipe(
        val angle: Float,
        val glare: Color?,
    ) : TransitionKind()

    data class Blinds(
        val slatWidth: Dp,
        val orientation: BlindsOrientation,
        val staggered: Boolean,
    ) : TransitionKind()

    data object Anvil : TransitionKind()

    data class Pop(
        val color: Color,
    ) : TransitionKind()

    data class Poof(
        val color: Color,
    ) : TransitionKind()

    data class Dissolve(
        val cellSize: Dp,
    ) : TransitionKind()

    data class Curtain(
        val orientation: BlindsOrientation,
    ) : TransitionKind()

    data class ShapeIris(
        val shape: Shape,
        val origin: TransformOrigin,
    ) : TransitionKind()

    data class Checker(
        val cellSize: Dp,
    ) : TransitionKind()

    data class Tear(
        val angle: Float,
        val toothSize: Dp,
    ) : TransitionKind()

    data class Blob(
        val origin: TransformOrigin,
    ) : TransitionKind()

    data class Shatter(
        val shards: Int,
    ) : TransitionKind()

    data class Disintegrate(
        val color: Color,
        val duration: Duration,
    ) : TransitionKind()
}
