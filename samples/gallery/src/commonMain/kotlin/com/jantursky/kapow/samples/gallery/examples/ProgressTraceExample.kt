package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ProgressEffect
import com.jantursky.kapow.progressEffect
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.math.roundToInt

/** An upload card whose edge is drawn around it as [progress] goes from 0 to 1. */
@Composable
fun ProgressTraceExample(
    progress: Float,
    color: Color,
    width: Dp,
    clockwise: Boolean = true,
    start: Float = 0f,
    tail: Float = 1f,
    gradient: Boolean = false,
) {
    Column(
        Modifier
            .size(width = 240.dp, height = 140.dp)
            .progressEffect(
                ProgressEffect.trace(
                    KapowShapes.card,
                    color,
                    width,
                    brush = if (gradient) Brush.linearGradient(listOf(color, KapowEffectColors.amber)) else null,
                    clockwise = clockwise,
                    start = start,
                    tail = tail,
                ),
            ) { progress }
            .clip(KapowShapes.card)
            .background(KapowTheme.colors.panel)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KapowText("photo.jpg", style = KapowTheme.type.subheading)
        KapowCaption(if (progress >= 1f) "Uploaded" else "Uploading, ${(progress * 100).roundToInt()} %")
    }
}
