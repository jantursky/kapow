package com.jantursky.kapow.multihaptic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jantursky.kapow.KapowFeedback
import com.jantursky.kapow.KapowFeedbackEvent
import top.ltfan.multihaptic.DelayType
import top.ltfan.multihaptic.HapticEffect
import top.ltfan.multihaptic.Primitive
import top.ltfan.multihaptic.PrimitiveType
import top.ltfan.multihaptic.compose.rememberVibrator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A [KapowFeedback] that plays each built-in event on the device's vibrator through multihaptic. Custom events
 * are ignored; handle them with your own [KapowFeedback] if you need them.
 *
 * ```
 * CompositionLocalProvider(LocalKapowFeedback provides rememberMultihapticFeedback()) { App() }
 * ```
 */
@Composable
public fun rememberMultihapticFeedback(): KapowFeedback {
    val vibrator = rememberVibrator()
    return remember(vibrator) { KapowFeedback { event -> hapticEffectFor(event)?.let(vibrator::vibrate) } }
}

/** The vibration for [event], built from multihaptic's predefined primitives, or null for a custom event. */
internal fun hapticEffectFor(event: KapowFeedbackEvent): HapticEffect? =
    when (event) {
        KapowFeedbackEvent.Selection -> {
            effect(tap(PrimitiveType.Tick))
        }

        KapowFeedbackEvent.ImpactLight -> {
            effect(tap(PrimitiveType.Click, 0.4f))
        }

        KapowFeedbackEvent.ImpactMedium -> {
            effect(tap(PrimitiveType.Click, 0.7f))
        }

        KapowFeedbackEvent.ImpactHeavy -> {
            effect(tap(PrimitiveType.Thud))
        }

        KapowFeedbackEvent.Success -> {
            effect(tap(PrimitiveType.QuickRise, 0.6f), tap(PrimitiveType.Click, after = GAP))
        }

        KapowFeedbackEvent.Warning -> {
            effect(tap(PrimitiveType.Click, 0.8f), tap(PrimitiveType.Click, 0.8f, GAP))
        }

        KapowFeedbackEvent.Error -> {
            effect(tap(PrimitiveType.Thud), tap(PrimitiveType.Click, 0.6f, GAP), tap(PrimitiveType.Click, 0.6f, GAP))
        }

        is KapowFeedbackEvent.Custom -> {
            null
        }
    }

private val GAP = 80.milliseconds

private fun tap(
    type: PrimitiveType,
    scale: Float = 1f,
    after: Duration = Duration.ZERO,
): Primitive = Primitive(type, scale, after, DelayType.Pause)

private fun effect(vararg primitives: Primitive): HapticEffect = HapticEffect(primitives.toList())
