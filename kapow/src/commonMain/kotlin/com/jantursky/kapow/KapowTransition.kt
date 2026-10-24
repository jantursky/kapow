package com.jantursky.kapow

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
) {
    override fun equals(other: Any?): Boolean = other is KapowEnter && kind == other.kind && duration == other.duration

    override fun hashCode(): Int = 31 * kind.hashCode() + duration.hashCode()

    override fun toString(): String = "KapowEnter($kind, $duration)"

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
) {
    override fun equals(other: Any?): Boolean = other is KapowExit && kind == other.kind && duration == other.duration

    override fun hashCode(): Int = 31 * kind.hashCode() + duration.hashCode()

    override fun toString(): String = "KapowExit($kind, $duration)"

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
    }
}

private val DefaultMaskDuration: Duration = 450.milliseconds

private fun Duration.checked(): Duration {
    require(isPositive()) { "duration must be positive" }
    return this
}

private fun Dp.checkedCell(): Dp {
    require(this >= 2.dp) { "cellSize must be at least 2 dp" }
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
}
