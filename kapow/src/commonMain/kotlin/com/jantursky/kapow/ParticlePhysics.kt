package com.jantursky.kapow

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode

/** Where [ParticlePhysics] starts particles, centered on the effect's own origin and sized to the element. */
@Immutable
public sealed class EmissionArea {
    /** Every particle starts at the origin. */
    public data object Point : EmissionArea()

    /** Particles start spread along one edge of the element. */
    public data class Edge(
        val side: EdgeSide,
    ) : EmissionArea()

    /** Particles start anywhere inside the element's bounds. */
    public data object Rect : EmissionArea()
}

/** A side of an element, for [EmissionArea.Edge]. [Start] and [End] follow the layout direction. */
public enum class EdgeSide { Top, Bottom, Start, End }

/** How a particle effect picks the color of each particle from its list. */
public enum class ColorPick {
    /** The colors in order, starting over after the last. */
    Sequential,

    /** A random color for each particle, from the effect's seed. */
    Random,
}

/**
 * How the particles of an effect launch and move. Every part is optional: a part left out keeps the effect's own
 * value, so `ParticlePhysics.Default` changes nothing. Parts an effect has no use for are ignored; its
 * documentation says which. Angles are in degrees clockwise from pointing right, so -90 is straight up.
 *
 * ```
 * ChangeEffect.spray(physics = ParticlePhysics.of(direction = -90f, spread = 40f, gravity = 600f))
 * ```
 */
@Immutable
public class ParticlePhysics internal constructor(
    internal val direction: Float?,
    internal val spread: Float?,
    internal val speed: ClosedFloatingPointRange<Float>?,
    internal val gravity: Float?,
    internal val drag: Float?,
    internal val lifetime: ClosedFloatingPointRange<Float>?,
    internal val size: ClosedFloatingPointRange<Float>?,
    internal val spin: ClosedFloatingPointRange<Float>?,
    internal val startScale: Float?,
    internal val endScale: Float?,
    internal val fade: Easing?,
    internal val emission: EmissionArea?,
    internal val blendMode: BlendMode?,
    internal val colorPick: ColorPick?,
) {
    override fun equals(other: Any?): Boolean =
        other is ParticlePhysics &&
            direction == other.direction &&
            spread == other.spread &&
            speed == other.speed &&
            gravity == other.gravity &&
            drag == other.drag &&
            lifetime == other.lifetime &&
            size == other.size &&
            spin == other.spin &&
            startScale == other.startScale &&
            endScale == other.endScale &&
            fade == other.fade &&
            emission == other.emission &&
            blendMode == other.blendMode &&
            colorPick == other.colorPick

    override fun hashCode(): Int =
        listOf(
            direction,
            spread,
            speed,
            gravity,
            drag,
            lifetime,
            size,
            spin,
            startScale,
            endScale,
            fade,
            emission,
            blendMode,
            colorPick,
        ).hashCode()

    override fun toString(): String =
        "ParticlePhysics(direction=$direction, spread=$spread, speed=$speed, gravity=$gravity, drag=$drag, " +
            "lifetime=$lifetime, size=$size, spin=$spin, startScale=$startScale, endScale=$endScale, fade=$fade, " +
            "emission=$emission, blendMode=$blendMode, colorPick=$colorPick)"

    public companion object {
        /** Every part left to the effect. */
        public val Default: ParticlePhysics = of()

        /**
         * @param direction the middle of the launch cone.
         * @param spread the width of the launch cone, from 0 (all one way) to 360 (every way).
         * @param speed the launch speed range, in dp per second.
         * @param gravity how fast particles accelerate downward, in dp per second squared. Negative floats up.
         * @param drag the fraction of speed kept after one second, from just above 0 to 1 (none).
         * @param lifetime how long a particle lives, in seconds, as a range.
         * @param size a factor on the effect's own particle sizes, as a range. 1..1 keeps them.
         * @param spin the turning speed range, in degrees per second.
         * @param startScale the size factor at birth, 0 or more. Particles grow or shrink to [endScale].
         * @param endScale the size factor at the end of life, 0 or more.
         * @param fade maps life progress (0 to 1) to how far the particle has faded (0 visible, 1 gone).
         * @param emission where particles start.
         * @param blendMode how particles blend with what is behind them.
         * @param colorPick how a color is chosen for each particle.
         */
        public fun of(
            direction: Float? = null,
            spread: Float? = null,
            speed: ClosedFloatingPointRange<Float>? = null,
            gravity: Float? = null,
            drag: Float? = null,
            lifetime: ClosedFloatingPointRange<Float>? = null,
            size: ClosedFloatingPointRange<Float>? = null,
            spin: ClosedFloatingPointRange<Float>? = null,
            startScale: Float? = null,
            endScale: Float? = null,
            fade: Easing? = null,
            emission: EmissionArea? = null,
            blendMode: BlendMode? = null,
            colorPick: ColorPick? = null,
        ): ParticlePhysics {
            require(spread == null || spread in 0f..360f) { "spread must be between 0 and 360" }
            require(speed == null || speed.start >= 0f) { "speed must not be negative" }
            require(drag == null || (drag > 0f && drag <= 1f)) { "drag must be above 0 and at most 1" }
            require(lifetime == null || lifetime.start > 0f) { "lifetime must be positive" }
            require(size == null || size.start >= 0f) { "size must not be negative" }
            require(startScale == null || startScale >= 0f) { "startScale must not be negative" }
            require(endScale == null || endScale >= 0f) { "endScale must not be negative" }
            return ParticlePhysics(
                direction,
                spread,
                speed,
                gravity,
                drag,
                lifetime,
                size,
                spin,
                startScale,
                endScale,
                fade,
                emission,
                blendMode,
                colorPick,
            )
        }
    }
}
