package com.jantursky.kapow.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.jantursky.kapow.KapowMotion
import com.jantursky.kapow.KapowTestApi
import com.jantursky.kapow.LocalKapowEffects
import com.jantursky.kapow.LocalKapowMotion
import kotlin.time.Duration

/** What Kapow effects do inside [KapowTest]. */
public enum class KapowTestMode(
    internal val motion: KapowMotion,
    internal val effects: Boolean,
) {
    /** Effects draw nothing and send no feedback, and transitions end in their final state at once. */
    Disabled(KapowMotion.System, false),

    /** Effects play their reduced-motion version, whatever the platform setting is. */
    Reduced(KapowMotion.Reduced, true),

    /** Effects play in full, whatever the platform setting is. */
    Full(KapowMotion.Full, true),
}

/** Runs [content] with Kapow effects in [mode]. The default, [KapowTestMode.Disabled], makes screens deterministic. */
@OptIn(KapowTestApi::class)
@Composable
public fun KapowTest(
    mode: KapowTestMode = KapowTestMode.Disabled,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalKapowEffects provides mode.effects,
        LocalKapowMotion provides mode.motion,
    ) { content() }
}

/** Sets [content] wrapped in [KapowTest] with [mode]. */
@OptIn(ExperimentalTestApi::class)
public fun ComposeUiTest.setKapowContent(
    mode: KapowTestMode = KapowTestMode.Disabled,
    content: @Composable () -> Unit,
) {
    setContent { KapowTest(mode, content) }
}

/**
 * Pauses the test clock's automatic advance, moves it by [duration] and lets the composition settle, so a test can
 * look at an effect at a known time.
 */
@OptIn(ExperimentalTestApi::class)
public fun ComposeUiTest.advanceKapowEffects(duration: Duration) {
    mainClock.autoAdvance = false
    mainClock.advanceTimeBy(duration.inWholeMilliseconds)
    waitForIdle()
}
