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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.PulseStyle
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A cart badge that sends out pulses each time [items] changes. */
@Composable
fun PulseExample(
    items: Int,
    shape: Shape,
    color: Color,
    fill: Boolean,
    pulses: Int,
) {
    val pulse = ChangeEffect.pulse(shape, color, if (fill) PulseStyle.Fill else PulseStyle.Stroke(), pulses)
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        KapowText("Cart", style = KapowTheme.type.subheading)
        Box(
            Modifier
                .size(44.dp)
                .changeEffect(items, pulse)
                .clip(shape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("$items", color = Color.Black)
        }
    }
}
