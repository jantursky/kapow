package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** A to-do row whose label is struck through when [done] turns true. Unticking it plays nothing. */
@Composable
fun StrikeExample(
    done: Boolean,
    color: Color,
    hold: Duration,
) {
    val colors = KapowTheme.colors
    Row(
        Modifier
            .width(260.dp)
            .clip(KapowShapes.control)
            .background(colors.panel)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(KapowShapes.pill)
                .then(
                    if (done) Modifier.background(color) else Modifier.border(1.5.dp, colors.slate, KapowShapes.pill),
                ),
        )
        KapowText(
            "Buy oat milk",
            color = if (done) colors.slate else colors.ink,
            modifier = Modifier.changeEffect(done, ChangeEffect.strike(color, hold = hold), isEnabled = done),
        )
    }
}
