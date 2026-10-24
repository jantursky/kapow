package com.jantursky.kapow.crease

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.jantursky.crease.rememberHinge
import com.jantursky.kapow.ProgressEffect
import com.jantursky.kapow.progressEffect

/**
 * Draws [effect] at a progress that follows the hinge angle: 0 at the start of [angleRange], 1 at its end, clamped
 * outside it. On devices and platforms without a hinge angle the element is left unchanged.
 *
 * The angle comes from Crease, so `ProvideFoldState` feeds a fixed or simulated angle in previews and tests.
 *
 * ```
 * Card(Modifier.hingeEffect(ProgressEffect.shine()))
 * ```
 */
@Composable
public fun Modifier.hingeEffect(
    effect: ProgressEffect,
    angleRange: ClosedFloatingPointRange<Float> = 0f..180f,
): Modifier {
    val angle = rememberHinge()?.angle ?: return this
    val progress = rememberUpdatedState(hingeProgress(angle, angleRange))
    return progressEffect(effect) { progress.value }
}

/** Maps a hinge angle in degrees linearly onto 0..1 over [range], clamped. */
internal fun hingeProgress(
    angle: Float,
    range: ClosedFloatingPointRange<Float>,
): Float {
    val span = range.endInclusive - range.start
    if (span <= 0f) return if (angle >= range.start) 1f else 0f
    return ((angle - range.start) / span).coerceIn(0f, 1f)
}
