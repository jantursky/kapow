package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A level badge that pops a comic burst out from behind itself each time [level] goes up. */
@Composable
fun BurstExample(
    level: Int,
    color: Color,
    spikes: Int,
    inFront: Boolean = false,
    outline: Dp = 0.dp,
    sizeScale: Float = 1f,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(44.dp)) {
        Box(
            Modifier
                .size(72.dp)
                .changeEffect(
                    level,
                    ChangeEffect.burst(color, spikes, sizeScale = sizeScale, inFront = inFront, outline = outline),
                ).background(KapowTheme.colors.panel, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("$level", style = KapowTheme.type.headingSm, color = KapowTheme.colors.title)
        }
        KapowCaption("Level $level")
    }
}
