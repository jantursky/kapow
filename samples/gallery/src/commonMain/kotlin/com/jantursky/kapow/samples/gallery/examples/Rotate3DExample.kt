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
import com.jantursky.kapow.SpinAxis
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A card that turns into view around [axis] and turns away again. */
@Composable
fun Rotate3DExample(
    visible: Boolean,
    axis: SpinAxis,
    duration: Duration,
    degrees: Float = 90f,
    shading: Float = 0.35f,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.rotate3D(axis, duration, degrees, shading = shading),
        exit = KapowExit.rotate3D(axis, duration, degrees, shading = shading),
    ) {
        Box(
            Modifier
                .size(width = 180.dp, height = 110.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.violet),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Card", color = Color.White)
        }
    }
}
