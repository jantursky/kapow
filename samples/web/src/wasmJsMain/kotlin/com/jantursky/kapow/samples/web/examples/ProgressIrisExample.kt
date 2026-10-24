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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ProgressEffect
import com.jantursky.kapow.progressEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText

/** A photo card that opens from [origin] as [progress] grows, like a lock screen wallpaper being revealed. */
@Composable
fun ProgressIrisExample(
    progress: Float,
    origin: TransformOrigin,
) {
    Box(
        Modifier
            .size(width = 240.dp, height = 160.dp)
            .progressEffect(ProgressEffect.iris(origin)) { progress }
            .clip(KapowShapes.card)
            .background(Brush.linearGradient(listOf(KapowEffectColors.amber, KapowEffectColors.coral))),
        contentAlignment = Alignment.Center,
    ) {
        KapowText("Sunrise", color = Color.Black)
    }
}
