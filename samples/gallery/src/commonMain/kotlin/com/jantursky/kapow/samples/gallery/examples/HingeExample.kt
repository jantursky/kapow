package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ProgressEffect
import com.jantursky.kapow.progressEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText

/**
 * A card whose shine follows the hinge [angle], 0 when closed and 180 when flat. A browser has no hinge, so the
 * angle comes from a slider. On a foldable, `Modifier.hingeEffect(effect)` from kapow-crease reads the real angle
 * and does the same mapping.
 */
@Composable
fun HingeExample(angle: Float) {
    Box(
        Modifier
            .size(width = 240.dp, height = 140.dp)
            .progressEffect(ProgressEffect.shine()) { angle / 180f }
            .clip(KapowShapes.card)
            .background(Brush.linearGradient(listOf(KapowEffectColors.cobalt, KapowEffectColors.violet))),
        contentAlignment = Alignment.Center,
    ) {
        KapowText("Unfold me", color = Color.White)
    }
}
