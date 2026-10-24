package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A file row that shatters into about [shards] pieces when it is deleted. */
@Composable
fun ShatterExample(
    visible: Boolean,
    shards: Int,
    duration: Duration,
) {
    KapowAnimatedVisibility(
        visible,
        exit = KapowExit.shatter(shards, duration),
    ) {
        Row(
            Modifier
                .size(width = 280.dp, height = 76.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.cobalt)
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(36.dp).clip(KapowShapes.control).background(KapowEffectColors.amber))
            Column {
                KapowText("Quarterly report.pdf", color = Color.White, maxLines = 1)
                KapowCaption("2.4 MB, edited yesterday", color = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}
