package com.jantursky.kapow.effects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Counts seconds on the frame clock for a looping effect. [seconds] is snapshot state, so reading it in the draw
 * phase redraws on every frame without recomposing. A long frame (a stall, a tab in the background) moves the
 * clock on by at most 1/30 s.
 */
internal class LoopClock {
    /** Seconds counted, at the pace the clock was started with. */
    var seconds by mutableDoubleStateOf(0.0)
        private set

    private var job: Job? = null

    val isRunning: Boolean get() = job?.isActive == true

    /** Starts counting at [pace] times real time, unless it already counts. [onFrame] runs after each frame. */
    fun start(
        scope: CoroutineScope,
        pace: Float = 1f,
        onFrame: (LoopClock) -> Unit = {},
    ) {
        if (isRunning) return
        job =
            scope.launch {
                var last = -1L
                while (isActive) {
                    withFrameNanos { now ->
                        if (last >= 0) seconds += ((now - last) / 1e9).coerceAtMost(MAX_STEP) * pace
                        last = now
                    }
                    onFrame(this@LoopClock)
                }
            }
    }

    fun reset() {
        seconds = 0.0
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val MAX_STEP = 1.0 / 30
    }
}
