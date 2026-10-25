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

/** The shape of the cells in [KapowEnter.dissolve] and [KapowExit.dissolve]. */
public enum class CellShape {
    /** Squares that tile the content. */
    Square,

    /** Circles as wide as their cell's diagonal, so neighbours overlap and cover the content when all are on. */
    Circle,

    /** Flat-topped hexagons that tile the content. */
    Hexagon,
}

/** The order in which the squares of [KapowEnter.checker] and [KapowExit.checker] open. */
public enum class CheckerOrder {
    /** The squares of one color open together, then the other color: the checkerboard's two waves. */
    Together,

    /** The squares open in a sweep from the top-left corner to the bottom-right (reversed on exit). */
    Diagonal,

    /** The squares open in a seeded random order. */
    Random,

    /** The squares open in a turn around the middle, from the center outward. */
    Spiral,
}

internal const val DEFAULT_DISSOLVE_SEED = 31
internal const val DEFAULT_CHECKER_SEED = 41

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

        /**
         * A circle that grows from [origin] until it covers the content.
         *
         * @param origin the point the circle grows from, as a fraction of the content's size.
         * @param duration how long the transition takes, 450 ms by default.
         * @param softEdge how wide the edge of the circle fades from shown to hidden. 0 gives a sharp edge. The fade
         *   has left the content when the circle is done, so it is fully shown at the end.
         */
        public fun iris(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
            softEdge: Dp = 0.dp,
        ): KapowEnter = KapowEnter(TransitionKind.Iris(origin, softEdge.checkedSoft()), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with softEdge.", level = DeprecationLevel.HIDDEN)
        public fun iris(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter = iris(origin, duration, 0.dp)

        /**
         * A sweep around [origin], clockwise from 12 o'clock, like a clock hand.
         *
         * @param origin the point the sweep turns around, as a fraction of the content's size.
         * @param duration how long the transition takes, 450 ms by default.
         * @param softEdge how wide the leading edge of the sweep fades, measured at the content's farthest corner; it
         *   is narrower closer to [origin]. 0 gives a sharp edge. The content is fully shown at the end.
         * @param counterClockwise sweeps counterclockwise from 12 o'clock instead.
         */
        public fun clock(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
            softEdge: Dp = 0.dp,
            counterClockwise: Boolean = false,
        ): KapowEnter =
            KapowEnter(TransitionKind.Clock(origin, softEdge.checkedSoft(), counterClockwise), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with softEdge and counterClockwise.", level = DeprecationLevel.HIDDEN)
        public fun clock(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter = clock(origin, duration, 0.dp, false)

        /**
         * A straight edge that moves across the content in the direction of [angle] (degrees, 0 = rightward).
         *
         * @param duration how long the transition takes, 450 ms by default.
         * @param softEdge how wide the edge fades from shown to hidden. 0 gives a sharp edge. The edge travels that
         *   much further, so the content is fully shown at the end.
         */
        public fun wipe(
            angle: Float = 0f,
            duration: Duration = DefaultMaskDuration,
            softEdge: Dp = 0.dp,
        ): KapowEnter =
            KapowEnter(TransitionKind.Wipe(angle, glare = null, softEdge = softEdge.checkedSoft()), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with softEdge.", level = DeprecationLevel.HIDDEN)
        public fun wipe(
            angle: Float = 0f,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter = wipe(angle, duration, 0.dp)

        /**
         * A [wipe] with a bright streak of [color] running along its edge.
         *
         * @param softEdge how wide the edge fades, as in [wipe].
         * @param width how wide the streak is. Left unspecified, it is a share of the content's size.
         * @param intensity how strong the streak is, from 0 (none) to 1 (the full [color]).
         */
        public fun glare(
            angle: Float = 45f,
            color: Color = Color.White,
            duration: Duration = DefaultMaskDuration,
            softEdge: Dp = 0.dp,
            width: Dp = Dp.Unspecified,
            intensity: Float = 1f,
        ): KapowEnter =
            KapowEnter(
                TransitionKind.Wipe(
                    angle,
                    color,
                    softEdge.checkedSoft(),
                    width.checkedGlareWidth(),
                    intensity.checkedIntensity(),
                ),
                duration.checked(),
            )

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with softEdge, width and intensity.", level = DeprecationLevel.HIDDEN)
        public fun glare(
            angle: Float = 45f,
            color: Color = Color.White,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter = glare(angle, color, duration, 0.dp, Dp.Unspecified, 1f)

        /**
         * Slats that open like venetian blinds. With [staggered], slats nearer the top (or start) open first.
         *
         * @param slatWidth the width of one slat, at least 5 dp. Ignored when [slatCount] is set.
         * @param staggered whether the slats open one after another instead of together.
         * @param slatCount how many slats there are, from 1 to 200, so the slats share the content's height (or
         *   width) evenly. Null sizes them by [slatWidth].
         * @param staggerAmount how far the start of the last slat trails the first, when [staggered]: 0 opens them
         *   together, 1 spreads the starts over half the transition, more spreads them further.
         * @param reverse opens the slats from the bottom (or end) up, when [staggered].
         */
        public fun blinds(
            slatWidth: Dp = 10.dp,
            orientation: BlindsOrientation = BlindsOrientation.Horizontal,
            staggered: Boolean = false,
            duration: Duration = DefaultMaskDuration,
            slatCount: Int? = null,
            staggerAmount: Float = 1f,
            reverse: Boolean = false,
        ): KapowEnter =
            KapowEnter(
                TransitionKind.Blinds(
                    slatWidth.coerceAtLeast(5.dp),
                    orientation,
                    staggered,
                    slatCount.checkedSlats(),
                    staggerAmount.checkedStagger(),
                    reverse,
                ),
                duration.checked(),
            )

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with slatCount, staggerAmount and reverse.", level = DeprecationLevel.HIDDEN)
        public fun blinds(
            slatWidth: Dp = 10.dp,
            orientation: BlindsOrientation = BlindsOrientation.Horizontal,
            staggered: Boolean = false,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter = blinds(slatWidth, orientation, staggered, duration, null, 1f, false)

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
         * @param cellShape the shape of the cells: squares, circles or hexagons. See [CellShape].
         * @param softness how gradually each cell turns on, from 0 to 1. At 0 a cell pops to full size at its moment;
         *   above it, each grows from its middle over that share of the transition, so 1 makes every cell grow
         *   together.
         * @param seed the seed of the random order. The same seed gives the same order on every target.
         */
        public fun dissolve(
            cellSize: Dp = 12.dp,
            duration: Duration = 600.milliseconds,
            cellShape: CellShape = CellShape.Square,
            softness: Float = 0f,
            seed: Int = DEFAULT_DISSOLVE_SEED,
        ): KapowEnter =
            KapowEnter(
                TransitionKind.Dissolve(cellSize.checkedCell(), cellShape, softness.checkedSoftness(), seed),
                duration.checked(),
            )

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with cellShape, softness and seed.", level = DeprecationLevel.HIDDEN)
        public fun dissolve(
            cellSize: Dp = 12.dp,
            duration: Duration = 600.milliseconds,
        ): KapowEnter = dissolve(cellSize, duration, CellShape.Square, 0f, DEFAULT_DISSOLVE_SEED)

        /**
         * Two halves part from the middle to reveal the content, like a stage curtain. With reduced motion it fades
         * in instead.
         *
         * @param orientation the direction of the seam between the halves, as in [blinds]: with
         *   [BlindsOrientation.Vertical] they slide apart to the left and right, with [BlindsOrientation.Horizontal]
         *   up and down.
         * @param duration how long the transition takes, 450 ms by default.
         * @param split where the seam between the halves sits, as a share of the content's width (or height) from 0
         *   to 1. 0.5 splits it in the middle; the halves then move just far enough to clear their own side.
         */
        public fun curtain(
            orientation: BlindsOrientation = BlindsOrientation.Vertical,
            duration: Duration = DefaultMaskDuration,
            split: Float = 0.5f,
        ): KapowEnter = KapowEnter(TransitionKind.Curtain(orientation, split.checkedSplit()), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with split.", level = DeprecationLevel.HIDDEN)
        public fun curtain(
            orientation: BlindsOrientation = BlindsOrientation.Vertical,
            duration: Duration = DefaultMaskDuration,
        ): KapowEnter = curtain(orientation, duration, 0.5f)

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
         * @param rotation how many degrees the shape turns about [origin] while it grows. 0 keeps it upright.
         * @param scale how much larger than needed the shape grows, at least 1. 1 covers the content just as the
         *   transition ends; more covers it sooner.
         */
        public fun shapeIris(
            shape: Shape = StarShape(),
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = 600.milliseconds,
            rotation: Float = 0f,
            scale: Float = 1f,
        ): KapowEnter =
            KapowEnter(TransitionKind.ShapeIris(shape, origin, rotation, scale.checkedScale()), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with rotation and scale.", level = DeprecationLevel.HIDDEN)
        public fun shapeIris(
            shape: Shape = StarShape(),
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = 600.milliseconds,
        ): KapowEnter = shapeIris(shape, origin, duration, 0f, 1f)

        /**
         * A checkerboard of squares grows over the content in two waves: the squares of one color open from their
         * middles, then the others. With reduced motion it fades in instead.
         *
         * @param cellSize the side of one square of the board, at least 2 dp. Large elements get bigger squares
         *   so a frame never draws more than a few thousand.
         * @param duration how long the transition takes, 600 ms by default.
         * @param order the order the squares open in. See [CheckerOrder].
         * @param seed the seed of [CheckerOrder.Random]. Ignored by the other orders.
         */
        public fun checker(
            cellSize: Dp = 24.dp,
            duration: Duration = 600.milliseconds,
            order: CheckerOrder = CheckerOrder.Together,
            seed: Int = DEFAULT_CHECKER_SEED,
        ): KapowEnter = KapowEnter(TransitionKind.Checker(cellSize.checkedCell(), order, seed), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with order and seed.", level = DeprecationLevel.HIDDEN)
        public fun checker(
            cellSize: Dp = 24.dp,
            duration: Duration = 600.milliseconds,
        ): KapowEnter = checker(cellSize, duration, CheckerOrder.Together, DEFAULT_CHECKER_SEED)

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

        /**
         * A circle that shrinks toward [origin].
         *
         * @param softEdge how wide the edge of the circle fades, as in [KapowEnter.iris].
         */
        public fun iris(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
            softEdge: Dp = 0.dp,
        ): KapowExit = KapowExit(TransitionKind.Iris(origin, softEdge.checkedSoft()), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with softEdge.", level = DeprecationLevel.HIDDEN)
        public fun iris(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit = iris(origin, duration, 0.dp)

        /**
         * A sweep around [origin] that takes the content away, counterclockwise back to 12 o'clock.
         *
         * @param softEdge how wide the trailing edge of the sweep fades, as in [KapowEnter.clock].
         * @param counterClockwise the direction of the enter counterpart; the exit takes the content away the
         *   other way around, so with `true` it sweeps clockwise back to 12 o'clock.
         */
        public fun clock(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
            softEdge: Dp = 0.dp,
            counterClockwise: Boolean = false,
        ): KapowExit =
            KapowExit(TransitionKind.Clock(origin, softEdge.checkedSoft(), counterClockwise), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with softEdge and counterClockwise.", level = DeprecationLevel.HIDDEN)
        public fun clock(
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit = clock(origin, duration, 0.dp, false)

        /**
         * A straight edge that covers the content, moving against [angle].
         *
         * @param softEdge how wide the edge fades, as in [KapowEnter.wipe].
         */
        public fun wipe(
            angle: Float = 0f,
            duration: Duration = DefaultMaskDuration,
            softEdge: Dp = 0.dp,
        ): KapowExit =
            KapowExit(TransitionKind.Wipe(angle, glare = null, softEdge = softEdge.checkedSoft()), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with softEdge.", level = DeprecationLevel.HIDDEN)
        public fun wipe(
            angle: Float = 0f,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit = wipe(angle, duration, 0.dp)

        /**
         * A [wipe] with a bright streak of [color] along its edge.
         *
         * @param softEdge how wide the edge fades, as in [KapowEnter.wipe].
         * @param width how wide the streak is. Left unspecified, it is a share of the content's size.
         * @param intensity how strong the streak is, from 0 (none) to 1 (the full [color]).
         */
        public fun glare(
            angle: Float = 45f,
            color: Color = Color.White,
            duration: Duration = DefaultMaskDuration,
            softEdge: Dp = 0.dp,
            width: Dp = Dp.Unspecified,
            intensity: Float = 1f,
        ): KapowExit =
            KapowExit(
                TransitionKind.Wipe(
                    angle,
                    color,
                    softEdge.checkedSoft(),
                    width.checkedGlareWidth(),
                    intensity.checkedIntensity(),
                ),
                duration.checked(),
            )

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with softEdge, width and intensity.", level = DeprecationLevel.HIDDEN)
        public fun glare(
            angle: Float = 45f,
            color: Color = Color.White,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit = glare(angle, color, duration, 0.dp, Dp.Unspecified, 1f)

        /**
         * Slats that close like venetian blinds.
         *
         * @param slatWidth the width of one slat, at least 5 dp. Ignored when [slatCount] is set.
         * @param staggered whether the slats close one after another instead of together.
         * @param slatCount how many slats there are, as in [KapowEnter.blinds].
         * @param staggerAmount how far the slats' timings are spread, when [staggered], as in [KapowEnter.blinds].
         * @param reverse starts from the bottom (or end), when [staggered].
         */
        public fun blinds(
            slatWidth: Dp = 10.dp,
            orientation: BlindsOrientation = BlindsOrientation.Horizontal,
            staggered: Boolean = false,
            duration: Duration = DefaultMaskDuration,
            slatCount: Int? = null,
            staggerAmount: Float = 1f,
            reverse: Boolean = false,
        ): KapowExit =
            KapowExit(
                TransitionKind.Blinds(
                    slatWidth.coerceAtLeast(5.dp),
                    orientation,
                    staggered,
                    slatCount.checkedSlats(),
                    staggerAmount.checkedStagger(),
                    reverse,
                ),
                duration.checked(),
            )

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with slatCount, staggerAmount and reverse.", level = DeprecationLevel.HIDDEN)
        public fun blinds(
            slatWidth: Dp = 10.dp,
            orientation: BlindsOrientation = BlindsOrientation.Horizontal,
            staggered: Boolean = false,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit = blinds(slatWidth, orientation, staggered, duration, null, 1f, false)

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
         * @param cellShape the shape of the cells, as in [KapowEnter.dissolve].
         * @param softness how gradually each cell turns off, from 0 to 1, as in [KapowEnter.dissolve].
         * @param seed the seed of the random order.
         */
        public fun dissolve(
            cellSize: Dp = 12.dp,
            duration: Duration = 600.milliseconds,
            cellShape: CellShape = CellShape.Square,
            softness: Float = 0f,
            seed: Int = DEFAULT_DISSOLVE_SEED,
        ): KapowExit =
            KapowExit(
                TransitionKind.Dissolve(cellSize.checkedCell(), cellShape, softness.checkedSoftness(), seed),
                duration.checked(),
            )

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with cellShape, softness and seed.", level = DeprecationLevel.HIDDEN)
        public fun dissolve(
            cellSize: Dp = 12.dp,
            duration: Duration = 600.milliseconds,
        ): KapowExit = dissolve(cellSize, duration, CellShape.Square, 0f, DEFAULT_DISSOLVE_SEED)

        /**
         * Two halves close in from the sides and meet in the middle, hiding the content. With reduced motion it fades
         * out instead.
         *
         * @param orientation the direction of the seam where the halves meet, as in [KapowEnter.curtain].
         * @param duration how long the transition takes, 450 ms by default.
         * @param split where the seam between the halves sits, as in [KapowEnter.curtain].
         */
        public fun curtain(
            orientation: BlindsOrientation = BlindsOrientation.Vertical,
            duration: Duration = DefaultMaskDuration,
            split: Float = 0.5f,
        ): KapowExit = KapowExit(TransitionKind.Curtain(orientation, split.checkedSplit()), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with split.", level = DeprecationLevel.HIDDEN)
        public fun curtain(
            orientation: BlindsOrientation = BlindsOrientation.Vertical,
            duration: Duration = DefaultMaskDuration,
        ): KapowExit = curtain(orientation, duration, 0.5f)

        /**
         * Like [iris], but the opening is [shape]: it shrinks toward [origin] until nothing is left. With reduced
         * motion it fades out instead.
         *
         * @param shape the opening, as in [KapowEnter.shapeIris].
         * @param origin the point the shape shrinks toward, as a fraction of the content's size.
         * @param duration how long the transition takes, 600 ms by default.
         * @param rotation how many degrees the shape turns about [origin] while it shrinks, as in
         *   [KapowEnter.shapeIris].
         * @param scale how much larger than needed the shape starts, at least 1, as in [KapowEnter.shapeIris].
         */
        public fun shapeIris(
            shape: Shape = StarShape(),
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = 600.milliseconds,
            rotation: Float = 0f,
            scale: Float = 1f,
        ): KapowExit =
            KapowExit(TransitionKind.ShapeIris(shape, origin, rotation, scale.checkedScale()), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with rotation and scale.", level = DeprecationLevel.HIDDEN)
        public fun shapeIris(
            shape: Shape = StarShape(),
            origin: TransformOrigin = TransformOrigin.Center,
            duration: Duration = 600.milliseconds,
        ): KapowExit = shapeIris(shape, origin, duration, 0f, 1f)

        /**
         * A checkerboard of squares shrinks away in two waves, the reverse of [KapowEnter.checker]. With reduced
         * motion it fades out instead.
         *
         * @param cellSize the side of one square of the board, at least 2 dp.
         * @param duration how long the transition takes, 600 ms by default.
         * @param order the order the squares close in, as in [KapowEnter.checker]. The exit runs it in reverse.
         * @param seed the seed of [CheckerOrder.Random].
         */
        public fun checker(
            cellSize: Dp = 24.dp,
            duration: Duration = 600.milliseconds,
            order: CheckerOrder = CheckerOrder.Together,
            seed: Int = DEFAULT_CHECKER_SEED,
        ): KapowExit = KapowExit(TransitionKind.Checker(cellSize.checkedCell(), order, seed), duration.checked())

        // Kept for binaries compiled against 1.0.0.
        @Deprecated("Use the overload with order and seed.", level = DeprecationLevel.HIDDEN)
        public fun checker(
            cellSize: Dp = 24.dp,
            duration: Duration = 600.milliseconds,
        ): KapowExit = checker(cellSize, duration, CheckerOrder.Together, DEFAULT_CHECKER_SEED)

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

private fun Dp.checkedSoft(): Dp {
    require(this >= 0.dp && value.isFinite()) { "softEdge must not be negative" }
    return this
}

private fun Dp.checkedGlareWidth(): Dp {
    require(this == Dp.Unspecified || (this > 0.dp && value.isFinite())) { "width must be positive" }
    return this
}

private fun Float.checkedIntensity(): Float {
    require(this in 0f..1f) { "intensity must be between 0 and 1" }
    return this
}

/** Null (sized by width) becomes 0; a count must be from 1 to 200. */
private fun Int?.checkedSlats(): Int {
    if (this == null) return 0
    require(this in 1..200) { "slatCount must be from 1 to 200" }
    return this
}

private fun Float.checkedStagger(): Float {
    require(this >= 0f && isFinite()) { "staggerAmount must not be negative" }
    return this
}

private fun Float.checkedSoftness(): Float {
    require(this in 0f..1f) { "softness must be between 0 and 1" }
    return this
}

private fun Float.checkedSplit(): Float {
    require(this in 0f..1f) { "split must be between 0 and 1" }
    return this
}

private fun Float.checkedScale(): Float {
    require(this >= 1f && isFinite()) { "scale must be at least 1" }
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
        val softEdge: Dp = 0.dp,
    ) : TransitionKind()

    data class Clock(
        val origin: TransformOrigin,
        val softEdge: Dp = 0.dp,
        val counterClockwise: Boolean = false,
    ) : TransitionKind()

    data class Wipe(
        val angle: Float,
        val glare: Color?,
        val softEdge: Dp = 0.dp,
        val glareWidth: Dp = Dp.Unspecified,
        val intensity: Float = 1f,
    ) : TransitionKind()

    data class Blinds(
        val slatWidth: Dp,
        val orientation: BlindsOrientation,
        val staggered: Boolean,
        val slatCount: Int = 0,
        val staggerAmount: Float = 1f,
        val reverse: Boolean = false,
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
        val cellShape: CellShape = CellShape.Square,
        val softness: Float = 0f,
        val seed: Int = DEFAULT_DISSOLVE_SEED,
    ) : TransitionKind()

    data class Curtain(
        val orientation: BlindsOrientation,
        val split: Float = 0.5f,
    ) : TransitionKind()

    data class ShapeIris(
        val shape: Shape,
        val origin: TransformOrigin,
        val rotation: Float = 0f,
        val scale: Float = 1f,
    ) : TransitionKind()

    data class Checker(
        val cellSize: Dp,
        val order: CheckerOrder = CheckerOrder.Together,
        val seed: Int = DEFAULT_CHECKER_SEED,
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
