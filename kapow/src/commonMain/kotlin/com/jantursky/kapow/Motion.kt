package com.jantursky.kapow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import com.jantursky.kapow.platform.platformReducedMotion
import kotlin.coroutines.CoroutineContext

/** How much motion Kapow effects may use. */
public enum class KapowMotion {
    /** Follow the platform's reduced-motion setting. */
    System,

    /** Always play full effects. */
    Full,

    /** Always play the reduced version: a short fade or highlight instead of motion and particles. */
    Reduced,
}

/** Overrides the reduced-motion setting for a part of the composition tree. Defaults to [KapowMotion.System]. */
public val LocalKapowMotion: ProvidableCompositionLocal<KapowMotion> = compositionLocalOf { KapowMotion.System }

/**
 * Whether effects here play their reduced version: [LocalKapowMotion] when it is [KapowMotion.Full] or
 * [KapowMotion.Reduced], otherwise the platform setting.
 */
@OptIn(KapowTestApi::class)
@Composable
public fun isReducedMotion(): Boolean =
    // With effects off, text and tilt effects take their end state, as under reduced motion.
    !LocalKapowEffects.current ||
        resolveReducedMotion(LocalKapowMotion.current, rememberCoroutineScope().coroutineContext.durationScale)

/**
 * Effects call this when they fire. Compose sets [MotionDurationScale] to 0 when animations are off on Android
 * and when Reduce Motion is on in iOS, so that covers Android versions the platform check can't see.
 */
internal fun resolveReducedMotion(
    motion: KapowMotion,
    durationScale: Float,
): Boolean =
    when (motion) {
        KapowMotion.Full -> false
        KapowMotion.Reduced -> true
        KapowMotion.System -> durationScale == 0f || platformReducedMotion()
    }

internal val CoroutineContext.durationScale: Float
    get() = this[MotionDurationScale]?.scaleFactor ?: 1f

/**
 * Opt-in to the switch that turns Kapow effects off, meant for UI tests. App code should not set it: use
 * `KapowTest` from `kapow-test` instead.
 */
@RequiresOptIn(
    message = "Switches Kapow effects off for tests. Use KapowTest from kapow-test instead of setting it in app code.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
public annotation class KapowTestApi

/**
 * Whether Kapow effects play here. Defaults to `true`. With `false`, effects draw nothing, send no feedback and
 * transitions end in their final state at once; the element's own content is untouched. For tests only.
 */
@KapowTestApi
public val LocalKapowEffects: ProvidableCompositionLocal<Boolean> = compositionLocalOf { true }
