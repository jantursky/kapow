package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A photo card that comes into focus as it appears and drifts out of focus as it goes. */
@Composable
fun BlurExample(
    visible: Boolean,
    duration: Duration,
    radius: Dp,
    scale: Float = 0.9f,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.blur(duration, radius, scale),
        exit = KapowExit.blur(duration, radius, scale),
    ) {
        Box(
            Modifier
                .size(width = 180.dp, height = 110.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.violet),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Photo", color = Color.White)
        }
    }
}
