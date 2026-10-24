package com.jantursky.kapow.samples.web.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** An achievement badge that pops in with a ring and a burst of [color]. */
@Composable
fun PopExample(
    visible: Boolean,
    color: Color,
    duration: Duration,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.pop(color, duration),
    ) {
        Box(
            Modifier
                .size(width = 140.dp, height = 84.dp)
                .clip(KapowShapes.card)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Achievement", color = if (color.luminance() > 0.4f) Color.Black else Color.White)
        }
    }
}
