package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.CheckerOrder
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** A level tile on a game board that a checkerboard opens on, and closes over when the level is locked again. */
@Composable
fun CheckerExample(
    visible: Boolean,
    cellSize: Dp,
    duration: Duration,
    order: CheckerOrder = CheckerOrder.Together,
    seed: Int = 41,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.checker(cellSize, duration, order, seed),
        exit = KapowExit.checker(cellSize, duration, order, seed),
    ) {
        Column(
            Modifier
                .size(160.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.cobalt),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KapowText("Level", color = Color.White)
            KapowText("12", style = KapowTheme.type.headingSm, color = Color.White)
            KapowText("Cleared", color = KapowEffectColors.amber)
        }
    }
}
