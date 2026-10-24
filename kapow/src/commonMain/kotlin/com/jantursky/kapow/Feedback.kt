package com.jantursky.kapow

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Plays haptic or sound feedback when an effect fires.
 *
 * Kapow doesn't make haptics itself. Provide an implementation backed by the library your app
 * already uses through [LocalKapowFeedback], or use the `kapow-multihaptic` artifact.
 */
public fun interface KapowFeedback {
    public fun perform(event: KapowFeedbackEvent)
}

/** The feedback an effect asks for. Handlers map each event to whatever their platform offers. */
@Immutable
public sealed class KapowFeedbackEvent {
    public data object Selection : KapowFeedbackEvent()

    public data object ImpactLight : KapowFeedbackEvent()

    public data object ImpactMedium : KapowFeedbackEvent()

    public data object ImpactHeavy : KapowFeedbackEvent()

    public data object Success : KapowFeedbackEvent()

    public data object Warning : KapowFeedbackEvent()

    public data object Error : KapowFeedbackEvent()

    /** An event your own handler understands, such as a sound named "coin". */
    public data class Custom(
        val name: String,
    ) : KapowFeedbackEvent()
}

/** The feedback handler for a part of the composition tree. `null`, the default, plays nothing. */
public val LocalKapowFeedback: androidx.compose.runtime.ProvidableCompositionLocal<KapowFeedback?> =
    staticCompositionLocalOf { null }
