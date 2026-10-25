package com.jantursky.kapow

import androidx.compose.runtime.Stable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.EffectNode
import com.jantursky.kapow.internal.ProgressRunner
import com.jantursky.kapow.internal.TriggerGate
import com.jantursky.kapow.internal.TunedContext
import com.jantursky.kapow.internal.awaitFrameTime
import com.jantursky.kapow.internal.skipsForReducedMotion
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Plays [effect] each time [value] changes to a value not equal to the previous one. Nothing plays on the first
 * composition. While [isEnabled] is false, changes are recorded but play nothing.
 *
 * ```
 * Text("$likes", Modifier.changeEffect(likes, ChangeEffect.spray()))
 * ```
 *
 * @param feedback asked of the [LocalKapowFeedback] handler each time the effect plays, also with reduced motion.
 */
@Stable
public fun Modifier.changeEffect(
    value: Any?,
    effect: ChangeEffect,
    isEnabled: Boolean = true,
    feedback: KapowFeedbackEvent? = null,
): Modifier = this then ChangeEffectElement(value, effect, isEnabled, feedback)

/**
 * Keeps [effect] on while [condition] is true, starting on the first composition if it already is. When the
 * condition turns false the effect winds down and the element returns to its normal look.
 */
@Stable
public fun Modifier.conditionalEffect(
    condition: Boolean,
    effect: ConditionalEffect,
): Modifier = this then ConditionalEffectElement(condition, effect)

/**
 * Draws [effect] at [progress], a value from 0 to 1 (clamped). The lambda is read while drawing, so a progress
 * backed by state, such as a scroll offset or an animation, updates the effect without recomposition.
 */
@Stable
public fun Modifier.progressEffect(
    effect: ProgressEffect,
    progress: () -> Float,
): Modifier = this then ProgressEffectElement(effect, progress)

private data class ChangeEffectElement(
    val value: Any?,
    val effect: ChangeEffect,
    val isEnabled: Boolean,
    val feedback: KapowFeedbackEvent?,
) : ModifierNodeElement<ChangeEffectNode>() {
    override fun create(): ChangeEffectNode = ChangeEffectNode(value, effect, isEnabled, feedback)

    override fun update(node: ChangeEffectNode) {
        node.update(value, effect, isEnabled, feedback)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "changeEffect"
        properties["value"] = value
        properties["effect"] = effect
        properties["isEnabled"] = isEnabled
        properties["feedback"] = feedback
    }
}

/** An effect with its wrappers peeled off: how, and how often, to play the innermost one. */
internal data class ChangePlan(
    val effect: ChangeEffect,
    val delay: Duration,
    val cooldown: Duration,
    val times: Int,
    val gap: Duration,
    val speed: Float,
    val mode: ReducedMotionMode,
) {
    /** Changes inside this window after an accepted one are ignored: the cooldown, or the whole series. */
    val gateWindow: Duration get() = maxOf(cooldown, gap * (times - 1))

    /** [context] with this plan's speed and reduced-motion mode, for the runner. */
    fun contextFor(context: EffectContext): EffectContext =
        if (speed == 1f && mode == ReducedMotionMode.Default) context else TunedContext(context, speed, mode)

    /** Fires [runner] [times] times, [gap] apart, unless reduced motion is on and the mode is `Nothing`. */
    suspend fun play(
        runner: ChangeRunner,
        context: EffectContext,
    ) {
        val tuned = contextFor(context)
        if (tuned.skipsForReducedMotion()) return
        repeat(times) { i ->
            if (i > 0) awaitFrameTime(gap)
            runner.fire(tuned)
        }
    }
}

/**
 * Peels [ChangeEffect.delay], [ChangeEffect.cooldown], [ChangeEffect.times], [ChangeEffect.speed] and
 * [ChangeEffect.reducedMotion] wrappers off an effect. Delays add and speeds multiply; for the rest the outermost
 * wrapper wins.
 */
internal fun ChangeEffect.plan(): ChangePlan {
    var effect = this
    var delay = Duration.ZERO
    var cooldown: Duration? = null
    var times: Int? = null
    var gap = Duration.ZERO
    var speed = 1f
    var mode: ReducedMotionMode? = null
    while (true) {
        when (val e = effect) {
            is ChangeEffect.Delayed -> {
                delay += e.delay
                effect = e.effect
            }

            is ChangeEffect.Tuned -> {
                if (cooldown == null) cooldown = e.customCooldown
                if (times == null && e.times != 1) {
                    times = e.times
                    gap = e.gap
                }
                speed *= e.speed
                if (mode == null) mode = e.mode
                effect = e.effect
            }

            else -> {
                break
            }
        }
    }
    return ChangePlan(
        effect,
        delay,
        cooldown ?: effect.cooldown,
        times ?: 1,
        gap,
        speed,
        mode ?: ReducedMotionMode.Default,
    )
}

internal class ChangeEffectNode(
    private var value: Any?,
    private var effect: ChangeEffect,
    private var isEnabled: Boolean,
    private var feedback: KapowFeedbackEvent?,
) : EffectNode<ChangeRunner>(effect.plan().effect.createRunner()) {
    private val gate = TriggerGate()

    // Set when the node is reused for other content: the next value it sees belongs to that content and is no change.
    private var isReused = false

    override fun newRunner(): ChangeRunner = effect.plan().effect.createRunner()

    override fun onReset() {
        super.onReset()
        isReused = true
        invalidateDraw()
    }

    override fun beforeDraw() {
        isReused = false
    }

    fun update(
        value: Any?,
        effect: ChangeEffect,
        isEnabled: Boolean,
        feedback: KapowFeedbackEvent?,
    ) {
        if (effect != this.effect) {
            this.effect = effect
            replaceRunner()
        }
        this.feedback = feedback
        this.isEnabled = isEnabled
        val changed = value != this.value && !isReused
        this.value = value
        isReused = false
        if (changed && isEnabled && isAttached) trigger()
    }

    /** Plays the effect once, subject to its cooldown and delay. Shared with [ConditionalEffect.repeat]. */
    fun trigger() {
        val plan = effect.plan()
        val runner = runner
        val event = feedback
        val changed = value
        coroutineScope.launch {
            val accepted = withFrameNanos { now -> gate.tryAccept(now, plan.gateWindow) }
            if (!accepted) return@launch
            awaitFrameTime(plan.delay)
            // The effect may have been replaced or turned off while the delay ran.
            if (runner !== this@ChangeEffectNode.runner || !isEnabled) return@launch
            perform(event)
            runner.changedValue = changed
            plan.play(runner, this@ChangeEffectNode)
        }
    }
}

private data class ConditionalEffectElement(
    val condition: Boolean,
    val effect: ConditionalEffect,
) : ModifierNodeElement<ConditionalEffectNode>() {
    override fun create(): ConditionalEffectNode = ConditionalEffectNode(condition, effect)

    override fun update(node: ConditionalEffectNode) {
        node.update(condition, effect)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "conditionalEffect"
        properties["condition"] = condition
        properties["effect"] = effect
    }
}

internal class ConditionalEffectNode(
    private var condition: Boolean,
    private var effect: ConditionalEffect,
) : EffectNode<ConditionalRunner>(effect.createRunner()) {
    override fun newRunner(): ConditionalRunner = effect.createRunner()

    override fun onAttach() {
        super.onAttach()
        if (condition) runner.setActive(this, true)
    }

    override fun onReset() {
        super.onReset()
        if (condition) runner.setActive(this, true)
    }

    fun update(
        condition: Boolean,
        effect: ConditionalEffect,
    ) {
        if (effect != this.effect) {
            this.effect = effect
            replaceRunner()
            if (condition && isAttached) runner.setActive(this, true)
        } else if (condition != this.condition && isAttached) {
            runner.setActive(this, condition)
        }
        this.condition = condition
    }
}

private class ProgressEffectElement(
    val effect: ProgressEffect,
    val progress: () -> Float,
) : ModifierNodeElement<ProgressEffectNode>() {
    override fun create(): ProgressEffectNode = ProgressEffectNode(effect, progress)

    override fun update(node: ProgressEffectNode) {
        node.update(effect, progress)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "progressEffect"
        properties["effect"] = effect
    }

    // A new lambda only updates the node. It matters when the lambda captures a plain value instead of reading state.
    override fun equals(other: Any?): Boolean =
        other is ProgressEffectElement && other.effect == effect && other.progress === progress

    override fun hashCode(): Int = 31 * effect.hashCode() + progress.hashCode()
}

internal class ProgressEffectNode(
    private var effect: ProgressEffect,
    private var progress: () -> Float,
) : EffectNode<ProgressRunner>(effect.createRunner().also { it.progress = progress }) {
    override fun newRunner(): ProgressRunner = effect.createRunner().also { it.progress = progress }

    fun update(
        effect: ProgressEffect,
        progress: () -> Float,
    ) {
        if (effect != this.effect) {
            this.effect = effect
            this.progress = progress
            replaceRunner()
        } else if (progress !== this.progress) {
            this.progress = progress
            runner.progress = progress
            invalidateDraw()
        }
    }
}
