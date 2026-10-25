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
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.SpinAxis
import com.jantursky.kapow.SpinRate
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A reward coin that spins each time [count] changes. */
@Composable
fun SpinExample(
    count: Int,
    axis: SpinAxis,
    rate: SpinRate,
    reverse: Boolean = false,
    shading: Float = 0.35f,
) {
    Box(
        Modifier
            .size(120.dp)
            .changeEffect(count, ChangeEffect.spin(axis, rate, reverse = reverse, shading = shading))
            .clip(KapowShapes.pill)
            .background(Brush.linearGradient(listOf(KapowEffectColors.amber, KapowEffectColors.coral))),
        contentAlignment = Alignment.Center,
    ) {
        KapowText("+10", style = KapowTheme.type.headingSm, color = Color.Black)
    }
}
