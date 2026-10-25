package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.BlindsOrientation
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** A prize card that a curtain opens on, and closes over again. */
@Composable
fun CurtainExample(
    visible: Boolean,
    orientation: BlindsOrientation,
    duration: Duration,
    split: Float = 0.5f,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.curtain(orientation, duration, split),
        exit = KapowExit.curtain(orientation, duration, split),
    ) {
        Column(
            Modifier
                .size(width = 240.dp, height = 160.dp)
                .clip(KapowShapes.card)
                .background(Brush.linearGradient(listOf(KapowEffectColors.violet, KapowEffectColors.cobalt))),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KapowText("You won", color = Color.White)
            KapowText("500 coins", style = KapowTheme.type.headingSm, color = Color.White)
        }
    }
}
