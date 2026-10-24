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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** A backup card that sends a comet around its edge each time [syncs] changes. */
@Composable
fun TraceExample(
    syncs: Int,
    shape: Shape,
    color: Color,
    duration: Duration,
) {
    Column(
        Modifier
            .size(width = 240.dp, height = 110.dp)
            .changeEffect(syncs, ChangeEffect.trace(shape, color, duration = duration))
            .clip(shape)
            .background(KapowTheme.colors.panel),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KapowText("Backup", style = KapowTheme.type.subheading)
        KapowCaption(if (syncs == 0) "Not synced yet" else "Synced $syncs × today")
    }
}
