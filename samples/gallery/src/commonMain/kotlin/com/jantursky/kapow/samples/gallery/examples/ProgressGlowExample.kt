package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ProgressEffect
import com.jantursky.kapow.progressEffect
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A charging tile whose halo grows with [progress]. */
@Composable
fun ProgressGlowExample(
    progress: Float,
    color: Color,
    radius: Dp,
    shape: Shape,
) {
    Box(
        Modifier
            .size(width = 220.dp, height = 120.dp)
            .progressEffect(ProgressEffect.glow(color, radius, shape)) { progress }
            .clip(shape)
            .background(KapowTheme.colors.panel),
        contentAlignment = Alignment.Center,
    ) {
        KapowText("${(progress * 100).toInt()} %", style = KapowTheme.type.headingSm, color = KapowTheme.colors.title)
    }
}
