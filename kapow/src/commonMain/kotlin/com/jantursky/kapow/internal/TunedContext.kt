package com.jantursky.kapow.internal

import com.jantursky.kapow.ReducedMotionMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/** [delegate] with the timing and reduced-motion settings of a wrapper laid over it. */
internal class TunedContext(
    private val delegate: EffectContext,
    override val speed: Float = 1f,
    override val reducedMotionMode: ReducedMotionMode = ReducedMotionMode.Default,
    override val fadeIn: Duration? = null,
    override val fadeOut: Duration? = null,
) : EffectContext by delegate

/** This duration at [speed] times the pace. */
internal fun Duration.at(speed: Float): Duration =
    if (speed ==
        1f
    ) {
        this
    } else {
        (inWholeNanoseconds / speed.toDouble()).toLong().nanoseconds
    }

/** Whether an effect with [mode] should draw nothing right now. */
internal fun EffectContext.skipsForReducedMotion(): Boolean =
    reducedMotionMode == ReducedMotionMode.Nothing && isReducedMotion()
