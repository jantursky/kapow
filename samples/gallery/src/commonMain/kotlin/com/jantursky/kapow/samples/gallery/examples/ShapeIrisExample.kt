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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** An achievement that opens through a growing [shape], such as a star, and closes back into it. */
@Composable
fun ShapeIrisExample(
    visible: Boolean,
    shape: Shape,
    origin: TransformOrigin,
    duration: Duration,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.shapeIris(shape, origin, duration),
        exit = KapowExit.shapeIris(shape, origin, duration),
    ) {
        Column(
            Modifier
                .size(width = 220.dp, height = 150.dp)
                .clip(KapowShapes.card)
                .background(Brush.radialGradient(listOf(KapowEffectColors.amber, KapowEffectColors.coral))),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KapowText("Achievement unlocked", color = Color.Black)
            KapowText("Seven-day streak", style = KapowTheme.type.headingSm, color = Color.Black)
        }
    }
}
