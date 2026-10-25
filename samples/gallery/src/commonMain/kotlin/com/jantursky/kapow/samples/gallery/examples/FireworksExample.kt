package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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

/** A habit streak card that sets off fireworks above itself each time [days] grows. */
@Composable
fun FireworksExample(
    days: Int,
    colors: List<Color>,
    count: Int,
    rocketHeight: Dp = 100.dp,
    burstRadius: Dp = Dp.Unspecified,
) {
    // The rocket climbs above the card, so the card sits low on the stage.
    Column(
        Modifier
            .padding(top = rocketHeight + 40.dp)
            .size(width = 200.dp, height = 88.dp)
            .changeEffect(days, ChangeEffect.fireworks(colors, count, rocketHeight, burstRadius = burstRadius))
            .background(KapowTheme.colors.panel, RoundedCornerShape(18.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        KapowText("$days-day streak", style = KapowTheme.type.subheading, color = KapowTheme.colors.title)
        KapowCaption("Reading, every evening")
    }
}
