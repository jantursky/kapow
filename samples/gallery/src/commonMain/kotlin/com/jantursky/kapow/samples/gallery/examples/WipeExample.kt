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

/** A banner that a straight edge wipes in at [angle] and back out. */
@Composable
fun WipeExample(
    visible: Boolean,
    angle: Float,
    duration: Duration,
    softEdge: Dp = 0.dp,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.wipe(angle, duration, softEdge),
        exit = KapowExit.wipe(angle, duration, softEdge),
    ) {
        Box(
            Modifier
                .size(width = 280.dp, height = 90.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.violet),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("New message", color = Color.White)
        }
    }
}
