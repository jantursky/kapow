package com.jantursky.kapow.effects

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.TriggerGate
import com.jantursky.kapow.internal.awaitFrameTime
import com.jantursky.kapow.withoutDelay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Fires a change effect's runner when the condition turns true and then every [every]. Turning the condition off
 * stops the schedule; a play already running finishes on its own.
 */
internal class RepeatRunner(
    effect: ChangeEffect,
    private val every: Duration,
) : ConditionalRunner() {
    private val inner: ChangeEffect
    private val delay: Duration
    private val runner: ChangeRunner
    private val gate = TriggerGate()
    private var job: Job? = null

    init {
        val (unwrapped, delay) = effect.withoutDelay()
        inner = unwrapped
        this.delay = delay
        runner = unwrapped.createRunner()
    }

    override val needsLayer: Boolean get() = runner.needsLayer

    override fun DrawScope.configureLayer(layer: GraphicsLayer) = with(runner) { configureLayer(layer) }

    override fun ContentDrawScope.drawEffect() = with(runner) { drawEffect() }

    override fun attach(context: EffectContext) = runner.attach(context)

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        job?.cancel()
        if (!active) return
        job =
            context.scope.launch {
                while (true) {
                    // The wrapped effect's cooldown still applies, so a short interval can't restart it mid-play.
                    if (withFrameNanos { gate.tryAccept(it, inner.cooldown) }) {
                        launch {
                            awaitFrameTime(delay)
                            runner.fire(context)
                        }
                    }
                    awaitFrameTime(every)
                }
            }
    }

    override fun stop() {
        job?.cancel()
        runner.stop()
    }
}
