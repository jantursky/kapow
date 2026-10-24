package com.jantursky.kapow.samples.web.examples

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

/** A timer card that a clock hand sweeps in around [origin] and back out. */
@Composable
fun ClockExample(
    visible: Boolean,
    origin: TransformOrigin,
    duration: Duration,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.clock(origin, duration),
        exit = KapowExit.clock(origin, duration),
    ) {
        Box(
            Modifier
                .size(width = 240.dp, height = 160.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.cobalt),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Time is up", color = Color.White)
        }
    }
}
