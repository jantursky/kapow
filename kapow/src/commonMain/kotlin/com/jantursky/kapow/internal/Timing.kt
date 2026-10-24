package com.jantursky.kapow.internal

import androidx.compose.runtime.withFrameNanos
import kotlin.time.Duration

/**
 * Accepts a trigger only if [cooldown] has passed since the last accepted one. Times come from the frame clock,
 * so tests drive it with a paused clock.
 */
internal class TriggerGate {
    private var lastNanos: Long? = null

    fun tryAccept(
        nowNanos: Long,
        cooldown: Duration,
    ): Boolean {
        val last = lastNanos
        if (last != null && nowNanos - last < cooldown.inWholeNanoseconds) return false
        lastNanos = nowNanos
        return true
    }
}

/** Waits [duration] on the frame clock rather than the dispatcher, so effects stay in step with frames. */
internal suspend fun awaitFrameTime(duration: Duration) {
    if (!duration.isPositive()) return
    val start = withFrameNanos { it }
    while (withFrameNanos { it } - start < duration.inWholeNanoseconds) {
        // Each iteration waits for the next frame.
    }
}
