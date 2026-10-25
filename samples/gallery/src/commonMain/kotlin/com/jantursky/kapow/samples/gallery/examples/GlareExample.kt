package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A "premium unlocked" card uncovered by a wipe with a streak of [color] along its edge. */
@Composable
fun GlareExample(
    visible: Boolean,
    angle: Float,
    color: Color,
    duration: Duration,
    softEdge: Dp = 0.dp,
    width: Dp = Dp.Unspecified,
    intensity: Float = 1f,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.glare(angle, color, duration, softEdge, width, intensity),
        exit = KapowExit.glare(angle, color, duration, softEdge, width, intensity),
    ) {
        Box(
            Modifier
                .size(width = 260.dp, height = 140.dp)
                .clip(KapowShapes.card)
                .background(Brush.linearGradient(listOf(KapowEffectColors.violet, KapowEffectColors.cobalt))),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Premium unlocked", color = Color.White)
        }
    }
}
