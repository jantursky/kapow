package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A voice assistant card that breathes slowly while it is [listening]. */
@Composable
fun BreatheExample(
    listening: Boolean,
    scale: Float,
    float: Dp,
    duration: Duration,
) {
    Column(
        Modifier
            .size(width = 180.dp, height = 120.dp)
            .conditionalEffect(listening, ConditionalEffect.breathe(scale, float, duration))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(KapowEffectColors.violet, KapowEffectColors.cobalt)))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
    ) {
        KapowText("Assistant", color = Color.White)
        KapowText(if (listening) "Listening" else "Idle", color = Color.White.copy(alpha = 0.75f))
    }
}
