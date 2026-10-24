package com.jantursky.kapow

import androidx.compose.runtime.Stable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectNode
import com.jantursky.kapow.internal.ProgressRunner
import com.jantursky.kapow.internal.TriggerGate
import com.jantursky.kapow.internal.awaitFrameTime
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
    override fun create(): ChangeEffectNode = ChangeEffectNode(value, effect, feedback)

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

/** Splits [ChangeEffect.delay] wrappers off an effect, adding up nested delays. */
internal fun ChangeEffect.withoutDelay(): Pair<ChangeEffect, Duration> {
    var effect = this
    var delay = Duration.ZERO
    while (effect is ChangeEffect.Delayed) {
        delay += effect.delay
        effect = effect.effect
    }
    return effect to delay
}

internal class ChangeEffectNode(
    private var value: Any?,
    private var effect: ChangeEffect,
    private var feedback: KapowFeedbackEvent?,
) : EffectNode<ChangeRunner>(effect.withoutDelay().first.createRunner()) {
    private val gate = TriggerGate()

    fun update(
        value: Any?,
        effect: ChangeEffect,
        isEnabled: Boolean,
        feedback: KapowFeedbackEvent?,
    ) {
        if (effect != this.effect) {
            this.effect = effect
            replaceRunner(effect.withoutDelay().first.createRunner())
        }
        this.feedback = feedback
        val changed = value != this.value
        this.value = value
        if (changed && isEnabled && isAttached) trigger()
    }

    /** Plays the effect once, subject to its cooldown and delay. Shared with [ConditionalEffect.repeat]. */
    fun trigger() {
        val (inner, delay) = effect.withoutDelay()
        val runner = runner
        val event = feedback
        coroutineScope.launch {
            val accepted = withFrameNanos { now -> gate.tryAccept(now, inner.cooldown) }
            if (!accepted) return@launch
            awaitFrameTime(delay)
            perform(event)
            runner.fire(this@ChangeEffectNode)
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
    override fun onAttach() {
        super.onAttach()
        if (condition) runner.setActive(this, true)
    }

    fun update(
        condition: Boolean,
        effect: ConditionalEffect,
    ) {
        if (effect != this.effect) {
            this.effect = effect
            replaceRunner(effect.createRunner())
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
    fun update(
        effect: ProgressEffect,
        progress: () -> Float,
    ) {
        if (effect != this.effect) {
            this.effect = effect
            this.progress = progress
            replaceRunner(effect.createRunner().also { it.progress = progress })
        } else if (progress !== this.progress) {
            this.progress = progress
            runner.progress = progress
            invalidateDraw()
        }
    }
}
