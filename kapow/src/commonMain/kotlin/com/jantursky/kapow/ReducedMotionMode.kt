package com.jantursky.kapow

/**
 * What one effect does when reduced motion is on, chosen per effect with the `reducedMotion(...)` wrappers. It
 * replaces the effect's own fallback; without a wrapper every effect keeps [Default].
 */
public enum class ReducedMotionMode {
    /** The effect's own fallback, as its documentation describes. */
    Default,

    /** The effect draws nothing and sends no work to the frame loop. Feedback events still fire. */
    Nothing,

    /** The effect's final look appears at once, without motion. Effects that end at rest draw nothing. */
    EndState,

    /** The effect's final look fades in and out over a short time instead of moving. */
    Fade,
}
