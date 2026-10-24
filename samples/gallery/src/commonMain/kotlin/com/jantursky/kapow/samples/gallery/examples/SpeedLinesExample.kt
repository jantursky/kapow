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
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** An incoming call whose avatar throws out action lines on every ring, each time [rings] changes. */
@Composable
fun SpeedLinesExample(
    rings: Int,
    color: Color,
    count: Int,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)) {
        Box(
            Modifier
                .size(64.dp)
                .changeEffect(rings, ChangeEffect.speedLines(color, count))
                .background(color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("AK", style = KapowTheme.type.subheading, color = Color.Black)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            KapowText("Alex Kim", style = KapowTheme.type.subheading, color = KapowTheme.colors.title)
            KapowCaption(if (rings == 0) "Calling" else "Ringing, $rings")
        }
    }
}
