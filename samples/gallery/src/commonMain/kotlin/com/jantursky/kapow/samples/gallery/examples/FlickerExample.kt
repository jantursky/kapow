package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A neon sign that flickers on and off. */
@Composable
fun FlickerExample(
    visible: Boolean,
    duration: Duration,
    steps: Int,
    seed: Int,
    dim: Float,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.flicker(duration, steps, seed, dim),
        exit = KapowExit.flicker(duration, steps, seed, dim),
    ) {
        Box(
            Modifier
                .size(width = 180.dp, height = 110.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.amber),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Open", color = Color.Black)
        }
    }
}
