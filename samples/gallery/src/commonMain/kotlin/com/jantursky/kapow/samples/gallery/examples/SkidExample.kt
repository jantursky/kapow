package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.SkidEdge
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A toast that skids in from [enterEdge] and out across [exitEdge]. */
@Composable
fun SkidExample(
    visible: Boolean,
    enterEdge: SkidEdge,
    exitEdge: SkidEdge,
    duration: Duration,
    overshoot: Float = 1f,
    lean: Float = 8f,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.skid(enterEdge, duration, overshoot, lean),
        exit = KapowExit.skid(exitEdge, duration, overshoot, lean),
    ) {
        Box(
            Modifier
                .size(width = 180.dp, height = 110.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.cobalt),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Saved", color = Color.White)
        }
    }
}
