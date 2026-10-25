package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.jantursky.kapow.ReducedMotionMode
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * A conditional effect that loops while it is on. [strength] fades from 0 to 1 when the condition turns true and
 * back when it turns false; [time] counts seconds on the frame clock until the fade-out ends. Both are snapshot
 * state for the draw and layer phases. With reduced motion ([isReduced]) the clock doesn't run and the strength
 * switches without a fade, so subclasses draw a still frame. The fades last [fadeMillis] unless `fadeIn(...)` or
 * `fadeOut(...)` set them, and the clock runs at the effect's `speed(...)`.
 */
internal abstract class FadingLoopRunner(
    private val fadeMillis: Int = 300,
    private val loops: Boolean = true,
) : ConditionalRunner() {
    private val fade = Animatable(0f)
    private var seconds by mutableDoubleStateOf(0.0)
    private var fadeJob: Job? = null
    private var clockJob: Job? = null
    private var pace = 1f

    /** Whether reduced motion was on when the effect last turned on. */
    protected var isReduced: Boolean = false
        private set

    /** How far the effect is faded in, from 0 to 1. */
    protected val strength: Float get() = fade.value

    /** Seconds the loop has run. It keeps counting across activations, so a loop resumes where it was. */
    protected val time: Double get() = seconds

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        fadeJob?.cancel()
        pace = context.speed
        if (active) {
            isReduced = context.isReducedMotion()
            if (isReduced) {
                clockJob?.cancel()
            } else if (loops) {
                startClock(context)
            }
        }
        val target = if (active) 1f else 0f
        val chosen = if (active) context.fadeIn else context.fadeOut
        val millis =
            chosen?.inWholeMilliseconds?.toInt() ?: fadeMillis.milliseconds
                .at(pace)
                .inWholeMilliseconds
                .toInt()
        val fades = !isReduced || context.reducedMotionMode == ReducedMotionMode.Fade
        fadeJob =
            context.scope.launch {
                if (!fades) fade.snapTo(target) else fade.animateTo(target, tween(millis))
                if (!active) clockJob?.cancel()
            }
    }

    private fun startClock(context: EffectContext) {
        if (clockJob?.isActive == true) return
        clockJob =
            context.scope.launch {
                var last = -1L
                while (true) {
                    withFrameNanos { now ->
                        // A long frame (a tab in the background, a stall) moves the loop on by at most 1/30 s.
                        if (last >= 0) seconds += ((now - last) / 1e9).coerceAtMost(MAX_STEP) * pace
                        last = now
                    }
                }
            }
    }

    override fun stop() {
        fadeJob?.cancel()
        clockJob?.cancel()
    }

    private companion object {
        const val MAX_STEP = 1.0 / 30
    }
}

/** Where a loop of [period] seconds is at [time], from 0 to 1. */
internal fun loopPhase(
    time: Double,
    period: Double,
): Float = if (period <= 0.0) 0f else ((time / period) % 1.0).toFloat()
