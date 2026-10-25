package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A "level up" alert that drops in, lands with a shake and kicks up dust. */
@Composable
fun AnvilExample(
    visible: Boolean,
    duration: Duration,
    dropHeight: Dp = Dp.Unspecified,
    shake: Float = 1f,
    dustColor: Color = Color(0xFFB0B0B0),
    dustCount: Int = 6,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.anvil(duration, dropHeight, shake, dustColor, dustCount),
    ) {
        Box(
            Modifier
                .size(width = 240.dp, height = 120.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.coral),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Level up", color = Color.Black)
        }
    }
}
