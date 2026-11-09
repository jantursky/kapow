package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** A sticker that boings in and out with a squash. */
@Composable
fun BoingExample(
    visible: Boolean,
    duration: Duration,
    overshoot: Float = 1f,
    squash: Float = 0.5f,
    origin: TransformOrigin = TransformOrigin.Center,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.boing(duration, overshoot, squash, origin),
        exit = KapowExit.boing(duration, overshoot, squash, origin),
    ) {
        Box(
            Modifier
                .size(width = 180.dp, height = 110.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.coral),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Sticker", color = Color.White)
        }
    }
}
