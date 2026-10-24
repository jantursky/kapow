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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.BlindsOrientation
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A sheet whose slats open and close like venetian blinds. */
@Composable
fun BlindsExample(
    visible: Boolean,
    slatWidth: Dp,
    orientation: BlindsOrientation,
    staggered: Boolean,
    duration: Duration,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.blinds(slatWidth, orientation, staggered, duration),
        exit = KapowExit.blinds(slatWidth, orientation, staggered, duration),
    ) {
        Box(
            Modifier
                .size(width = 260.dp, height = 160.dp)
                .clip(KapowShapes.card)
                .background(Brush.linearGradient(listOf(KapowEffectColors.amber, KapowEffectColors.coral))),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Share", color = Color.Black)
        }
    }
}
