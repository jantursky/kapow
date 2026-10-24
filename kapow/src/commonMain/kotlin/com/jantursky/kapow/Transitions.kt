package com.jantursky.kapow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import com.jantursky.kapow.internal.EffectNode
import com.jantursky.kapow.internal.TransitionRunner
import com.jantursky.kapow.transitions.createRunner
import kotlinx.coroutines.flow.drop
import kotlin.time.Duration

/**
 * Applies Kapow transitions to content inside an `AnimatedVisibility` or `AnimatedContent`. Pass the visibility
 * scope as [scope]. The transition animates on the scope's own transition, so the container keeps the content
 * until the exit has finished.
 *
 * Compose's `EnterTransition` and `ExitTransition` can't be extended, so Kapow's run alongside them: set the
 * stock ones to `EnterTransition.None` / `ExitTransition.None`, or combine them, for example with `fadeIn()`.
 *
 * ```
 * AnimatedVisibility(visible, enter = EnterTransition.None, exit = ExitTransition.None) {
 *     Card(Modifier.kapowTransition(this, enter = KapowEnter.iris(), exit = KapowExit.poof()))
 * }
 * ```
 *
 * With reduced motion, every Kapow transition becomes a fade over the same duration or shorter.
 *
 * @param feedback asked of the [LocalKapowFeedback] handler when the content starts to enter or exit.
 */
@Composable
public fun Modifier.kapowTransition(
    scope: AnimatedVisibilityScope,
    enter: KapowEnter = KapowEnter.None,
    exit: KapowExit = KapowExit.None,
    feedback: KapowFeedbackEvent? = null,
): Modifier {
    val transition = scope.transition
    val reduced = isReducedMotion()
    val progress: State<Float> =
        transition.animateFloat(
            transitionSpec = {
                if (targetState ==
                    EnterExitState.Visible
                ) {
                    spec(enter.kind, enter.duration)
                } else {
                    spec(exit.kind, exit.duration)
                }
            },
            label = "kapowTransition",
        ) { if (it == EnterExitState.Visible) 1f else 0f }
    val handler = LocalKapowFeedback.current
    val event = rememberUpdatedState(feedback)
    LaunchedEffect(transition) {
        snapshotFlow { transition.targetState }.drop(1).collect { event.value?.let { handler?.perform(it) } }
    }
    return this then
        TransitionElement(
            enter = enter.kind,
            exit = exit.kind,
            reduced = reduced,
            progress = progress,
            entering = { transition.targetState == EnterExitState.Visible },
        )
}

/**
 * `AnimatedVisibility` with Kapow transitions. [stockEnter] and [stockExit] add Compose transitions on top, such
 * as `fadeIn()` or `expandVertically()`.
 */
@Composable
public fun KapowAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: KapowEnter = KapowEnter.None,
    exit: KapowExit = KapowExit.None,
    stockEnter: EnterTransition = EnterTransition.None,
    stockExit: ExitTransition = ExitTransition.None,
    feedback: KapowFeedbackEvent? = null,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(visible, modifier, stockEnter, stockExit) {
        androidx.compose.foundation.layout
            .Box(Modifier.kapowTransition(this, enter, exit, feedback)) { content() }
    }
}

private fun spec(
    kind: TransitionKind,
    duration: Duration,
): FiniteAnimationSpec<Float> {
    val millis = duration.inWholeMilliseconds.toInt()
    return when (kind) {
        TransitionKind.None -> snap()

        // These run their own curves on a linear clock.
        TransitionKind.Anvil, is TransitionKind.Pop, is TransitionKind.Poof -> tween(millis, easing = LinearEasing)

        else -> tween(millis, easing = FastOutSlowInEasing)
    }
}

private class TransitionElement(
    val enter: TransitionKind,
    val exit: TransitionKind,
    val reduced: Boolean,
    val progress: State<Float>,
    val entering: () -> Boolean,
) : ModifierNodeElement<TransitionNode>() {
    override fun create(): TransitionNode = TransitionNode(enter, exit, reduced).also { it.bind(progress, entering) }

    override fun update(node: TransitionNode) {
        node.update(enter, exit, reduced)
        node.bind(progress, entering)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "kapowTransition"
        properties["enter"] = enter
        properties["exit"] = exit
    }

    override fun equals(other: Any?): Boolean =
        other is TransitionElement && other.enter == enter && other.exit == exit && other.reduced == reduced &&
            other.progress === progress

    override fun hashCode(): Int =
        ((enter.hashCode() * 31 + exit.hashCode()) * 31 + reduced.hashCode()) * 31 + progress.hashCode()
}

private class TransitionNode(
    private var enter: TransitionKind,
    private var exit: TransitionKind,
    private var reduced: Boolean,
) : EffectNode<TransitionRunner>(createRunner(enter, exit, reduced)) {
    fun bind(
        progress: State<Float>,
        entering: () -> Boolean,
    ) {
        runner.progress = { progress.value }
        runner.entering = entering
    }

    fun update(
        enter: TransitionKind,
        exit: TransitionKind,
        reduced: Boolean,
    ) {
        if (enter == this.enter && exit == this.exit && reduced == this.reduced) return
        this.enter = enter
        this.exit = exit
        this.reduced = reduced
        val next = createRunner(enter, exit, reduced)
        next.progress = runner.progress
        next.entering = runner.entering
        replaceRunner(next)
    }
}
