package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A card that sends a band of light across itself each time [count] changes. */
@Composable
fun ShineExample(
    count: Int,
    angle: Float,
    duration: Duration,
    bands: Int = 1,
    softness: Float = 1f,
) {
    Box(
        modifier =
            Modifier
                .size(width = 240.dp, height = 140.dp)
                .changeEffect(
                    count,
                    ChangeEffect.shine(angle = angle, duration = duration, softness = softness, bands = bands),
                ).clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(KapowEffectColors.amber, KapowEffectColors.coral))),
        contentAlignment = Alignment.Center,
    ) {
        KapowText("Kapow", color = Color.Black)
    }
}
