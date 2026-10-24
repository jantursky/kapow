package com.jantursky.kapow.samples.web.examples

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

/** A membership card whose band of light follows [progress], as it could follow a scroll position. */
@Composable
fun ProgressShineExample(
    progress: Float,
    angle: Float,
    color: Color,
) {
    Box(
        Modifier
            .size(width = 240.dp, height = 140.dp)
            .progressEffect(ProgressEffect.shine(angle, color)) { progress }
            .clip(KapowShapes.card)
            .background(Brush.linearGradient(listOf(KapowEffectColors.violet, KapowEffectColors.cobalt))),
        contentAlignment = Alignment.Center,
    ) {
        KapowText("Member", color = Color.White)
    }
}
