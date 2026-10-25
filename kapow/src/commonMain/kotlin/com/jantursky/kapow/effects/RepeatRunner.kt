package com.jantursky.kapow.effects

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.TriggerGate
import com.jantursky.kapow.internal.awaitFrameTime
import com.jantursky.kapow.plan
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration

/**
 * The wait before the next play, for plays [every] apart with each wait changed by up to
 * [jitter] times [every] either way, as [unit] (-1 to 1) says. Never shorter than [minimum].
 */
internal fun repeatWait(
    every: Duration,
    jitter: Float,
    unit: Float,
    minimum: Duration,
): Duration = (every * (1.0 + jitter * unit.coerceIn(-1f, 1f))).coerceAtLeast(minimum)

/**
 * Fires a change effect's runner when the condition turns true, after [initialDelay], and then every [every] (give
 * or take [jitter] of it), at most [maxCount] times in all. Turning the condition off stops the schedule; a play
 * already running finishes on its own.
 */
internal class RepeatRunner(
    effect: ChangeEffect,
    private val every: Duration,
    private val initialDelay: Duration = Duration.ZERO,
    private val maxCount: Int? = null,
    private val jitter: Float = 0f,
    private val seed: Int = 0,
) : ConditionalRunner() {
    private val plan = effect.plan()
    private val runner: ChangeRunner = plan.effect.createRunner()
    private val gate = TriggerGate()
    private var job: Job? = null

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
        val random = Random(seed)
        val pace = context.speed.toDouble()
        job =
            context.scope.launch {
                awaitFrameTime(initialDelay / pace)
                var played = 0
                while (maxCount == null || played < maxCount) {
                    // The wrapped effect's cooldown still applies, so a short interval can't restart it mid-play.
                    if (withFrameNanos { gate.tryAccept(it, plan.gateWindow) }) {
                        launch {
                            awaitFrameTime(plan.delay)
                            plan.play(runner, context)
                        }
                    }
                    played++
                    if (maxCount != null && played >= maxCount) break
                    val unit = if (jitter > 0f) random.nextFloat() * 2f - 1f else 0f
                    awaitFrameTime(repeatWait(every / pace, jitter, unit, ConditionalEffect.MinRepeatInterval))
                }
            }
    }

    override fun stop() {
        job?.cancel()
        runner.stop()
    }
}
