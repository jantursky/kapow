package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.ParticleContent
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A cart badge that floats [label] up each time [items] changes. */
@Composable
fun RiseExample(
    items: Int,
    label: String,
    distance: Dp = Dp.Unspecified,
    drift: Float = 0f,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        KapowText("Cart", style = KapowTheme.type.subheading)
        Box(
            Modifier
                .size(44.dp)
                .changeEffect(items, ChangeEffect.rise(ParticleContent.text(label), distance = distance, drift = drift))
                .clip(KapowShapes.pill)
                .background(KapowEffectColors.amber),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("$items", color = Color.Black)
        }
    }
}
