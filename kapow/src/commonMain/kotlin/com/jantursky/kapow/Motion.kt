package com.jantursky.kapow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
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
@Composable
public fun isReducedMotion(): Boolean = resolveReducedMotion(LocalKapowMotion.current, durationScale = 1f)

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
