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
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A message row that vanishes in a puff of [color] smoke when it is deleted. */
@Composable
fun PoofExample(
    visible: Boolean,
    color: Color,
    duration: Duration,
    puffCount: Int = 10,
    puffSize: Dp = 28.dp,
    direction: Float? = null,
) {
    KapowAnimatedVisibility(
        visible,
        exit = KapowExit.poof(color, duration, puffCount, puffSize, direction),
    ) {
        Box(
            Modifier
                .size(width = 260.dp, height = 70.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.cobalt),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Message deleted", color = Color.White)
        }
    }
}
