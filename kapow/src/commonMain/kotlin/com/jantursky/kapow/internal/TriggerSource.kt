package com.jantursky.kapow.internal

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.snapshotFlow
import com.jantursky.kapow.KapowInteraction
import com.jantursky.kapow.KapowTrigger
import kotlinx.coroutines.flow.drop

/**
 * Something other than a changing value that plays a change effect. [collect] is started undispatched on attach, so
 * what it observes first is the state at attach and later events are not missed.
 */
internal sealed interface TriggerSource {
    suspend fun collect(onFire: () -> Unit)
}

/** Fires for each [KapowTrigger.fire], or once for a burst within a frame. */
internal data class TriggerFire(
    val trigger: KapowTrigger,
) : TriggerSource {
    override suspend fun collect(onFire: () -> Unit) {
        // The first emission is the count at attach, which is no fire.
        snapshotFlow { trigger.count }.drop(1).collect { onFire() }
    }
}

/** Fires each time [interaction] starts on [source]. */
internal data class InteractionFire(
    val source: InteractionSource,
    val interaction: KapowInteraction,
) : TriggerSource {
    override suspend fun collect(onFire: () -> Unit) {
        val tracker = InteractionTracker(interaction)
        source.interactions.collect { if (tracker.onInteraction(it) && tracker.isActive) onFire() }
    }
}

/** Follows one kind of [Interaction] the way foundation's `collectIsPressedAsState` and its siblings do. */
internal class InteractionTracker(
    private val kind: KapowInteraction,
) {
    private val active = mutableSetOf<Interaction>()

    val isActive: Boolean get() = active.isNotEmpty()

    /** Takes in [interaction] and tells whether [isActive] changed because of it. */
    fun onInteraction(interaction: Interaction): Boolean {
        val before = isActive
        when (kind) {
            KapowInteraction.Pressed -> {
                when (interaction) {
                    is PressInteraction.Press -> active += interaction
                    is PressInteraction.Release -> active -= interaction.press
                    is PressInteraction.Cancel -> active -= interaction.press
                }
            }

            KapowInteraction.Hovered -> {
                when (interaction) {
                    is HoverInteraction.Enter -> active += interaction
                    is HoverInteraction.Exit -> active -= interaction.enter
                }
            }

            KapowInteraction.Focused -> {
                when (interaction) {
                    is FocusInteraction.Focus -> active += interaction
                    is FocusInteraction.Unfocus -> active -= interaction.focus
                }
            }

            KapowInteraction.Dragged -> {
                when (interaction) {
                    is DragInteraction.Start -> active += interaction
                    is DragInteraction.Stop -> active -= interaction.start
                    is DragInteraction.Cancel -> active -= interaction.start
                }
            }
        }
        return isActive != before
    }
}
