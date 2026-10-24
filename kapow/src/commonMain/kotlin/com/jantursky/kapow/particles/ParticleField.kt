package com.jantursky.kapow.particles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The particles one node draws: the particle layer's host node, or an emitting element without a layer above
 * it. One frame loop steps every batch and stops when the last particle is gone.
 */
internal class ParticleField {
    private val batches = mutableListOf<ParticleBatch>()
    private var frame by mutableIntStateOf(0)
    private var loop: Job? = null

    val isIdle: Boolean get() = batches.isEmpty()

    fun add(
        batch: ParticleBatch,
        scope: CoroutineScope,
    ) {
        batches += batch
        frame++
        if (loop?.isActive == true) return
        loop =
            scope.launch {
                var last = -1L
                while (batches.isNotEmpty()) {
                    withFrameNanos { now ->
                        // A long frame (a tab in the background, a stall) moves particles at most 1/30 s.
                        val seconds = if (last < 0) 0f else ((now - last) / 1e9f).coerceAtMost(MaxStep)
                        last = now
                        batches.removeAll { !it.step(seconds) }
                        frame++
                    }
                }
            }
    }

    fun draw(scope: DrawScope) {
        // Reading the frame counter subscribes the draw phase, so each step redraws without recomposition.
        if (frame < 0) return
        batches.forEach { it.draw(scope) }
    }

    fun clear() {
        loop?.cancel()
        loop = null
        batches.clear()
    }

    private companion object {
        const val MaxStep = 1f / 30f
    }
}
