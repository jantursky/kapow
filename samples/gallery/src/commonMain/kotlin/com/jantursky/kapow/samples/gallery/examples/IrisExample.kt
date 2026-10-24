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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A photo card that opens like a camera iris from [origin] and closes back into it. */
@Composable
fun IrisExample(
    visible: Boolean,
    origin: TransformOrigin,
    duration: Duration,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.iris(origin, duration),
        exit = KapowExit.iris(origin, duration),
    ) {
        Box(
            Modifier
                .size(width = 240.dp, height = 160.dp)
                .clip(KapowShapes.card)
                .background(Brush.linearGradient(listOf(KapowEffectColors.amber, KapowEffectColors.coral))),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Sunrise", color = Color.Black)
        }
    }
}
