package com.jantursky.kapow.samples.web.examples

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
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A plan card that glows while it is [selected]. */
@Composable
fun ConditionalGlowExample(
    selected: Boolean,
    color: Color,
    radius: Dp,
    shape: Shape,
) {
    Box(
        Modifier
            .size(width = 220.dp, height = 120.dp)
            .conditionalEffect(selected, ConditionalEffect.glow(color, radius, shape))
            .clip(shape)
            .background(KapowTheme.colors.panel),
        contentAlignment = Alignment.Center,
    ) {
        KapowText("Team plan", style = KapowTheme.type.subheading, color = KapowTheme.colors.title)
    }
}
