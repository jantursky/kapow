package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText

/** A payment card that turns gray while it is [disabled], such as when it has expired. */
@Composable
fun GrayscaleExample(
    disabled: Boolean,
    amount: Float,
    tint: Color? = null,
    dim: Float = 0f,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .width(230.dp)
            .conditionalEffect(disabled, ConditionalEffect.grayscale(amount, tint, dim))
            .clip(shape)
            .background(Brush.linearGradient(listOf(KapowEffectColors.coral, KapowEffectColors.violet)))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // The card network's two overlapping circles.
        Box {
            Box(Modifier.size(24.dp).clip(CircleShape).background(KapowEffectColors.amber))
            Box(
                Modifier
                    .offset(
                        x = 14.dp,
                    ).size(24.dp)
                    .clip(CircleShape)
                    .background(KapowEffectColors.cobalt.copy(0.85f)),
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            KapowText("•••• 4242", color = Color.White, modifier = Modifier.weight(1f))
            KapowText(if (disabled) "Expired" else "09/29", color = Color.White.copy(alpha = 0.8f))
        }
    }
}
