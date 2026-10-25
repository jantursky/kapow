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
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A "Pro" card that flares a halo each time [count] changes, such as when the plan is upgraded. */
@Composable
fun GlowExample(
    count: Int,
    color: Color,
    radius: Dp,
    shape: Shape,
    spread: Dp = 0.dp,
    innerGlow: Float = 0f,
) {
    Box(
        Modifier
            .size(width = 220.dp, height = 120.dp)
            .changeEffect(count, ChangeEffect.glow(color, radius, shape, spread = spread, innerGlow = innerGlow))
            .clip(shape)
            .background(KapowTheme.colors.panel),
        contentAlignment = Alignment.Center,
    ) {
        KapowText("Pro", style = KapowTheme.type.headingSm, color = KapowTheme.colors.title)
    }
}
