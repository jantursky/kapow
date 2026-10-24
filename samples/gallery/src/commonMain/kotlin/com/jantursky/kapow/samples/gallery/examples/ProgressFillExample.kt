package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ProgressEffect
import com.jantursky.kapow.progressEffect
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.math.roundToInt

/** A download card that fills up with liquid as [progress] goes from 0 to 1. */
@Composable
fun ProgressFillExample(
    progress: Float,
    color: Color,
    wave: Dp,
) {
    Column(
        Modifier
            .size(width = 240.dp, height = 140.dp)
            .progressEffect(ProgressEffect.fill(color.copy(alpha = 0.6f), wave)) { progress }
            .clip(KapowShapes.card)
            .background(KapowTheme.colors.panel)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KapowText("Kapow-1.0.zip", style = KapowTheme.type.subheading)
        KapowCaption(if (progress >= 1f) "Downloaded" else "Downloading, ${(progress * 100).roundToInt()} %")
    }
}
