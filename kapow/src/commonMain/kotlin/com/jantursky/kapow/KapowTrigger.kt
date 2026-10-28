package com.jantursky.kapow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot

/**
 * Plays change effects on demand. Pass it to `Modifier.changeEffect(trigger, effect)` and call [fire] from a click
 * handler, a view model event or anywhere else; each call plays the effect once on every element that uses the
 * trigger. Firing never recomposes anything.
 *
 * Fires that arrive within one frame play once. A trigger that is fired before any element uses it plays nothing
 * later, and an element ignores fires that happened before it was composed.
 */
@Stable
public class KapowTrigger internal constructor() {
    internal var count: Long by mutableLongStateOf(0L)
        private set

    /** Plays the effect of every element that uses this trigger. Safe to call from any thread. */
    public fun fire() {
        Snapshot.withMutableSnapshot { count += 1 }
    }
}

/** Remembers a [KapowTrigger] for the lifetime of the composition. */
@Composable
public fun rememberKapowTrigger(): KapowTrigger = remember { KapowTrigger() }

/** The interaction of an `InteractionSource` that drives an effect. */
public enum class KapowInteraction {
    /** A press, from the pointer going down until it is released or cancelled. */
    Pressed,

    /** A pointer hovering over the element, as reported by `hoverable`. */
    Hovered,

    /** The element holding keyboard or accessibility focus, as reported by `focusable`. */
    Focused,

    /** A drag, from its start until it stops or is cancelled. */
    Dragged,
}

/** Which way a Boolean has to change to play an effect. */
public enum class KapowEdge {
    /** When the condition turns true. */
    Rising,

    /** When the condition turns false. */
    Falling,
}
