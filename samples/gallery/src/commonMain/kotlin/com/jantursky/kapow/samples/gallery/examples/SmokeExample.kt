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
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText

/** A "Hot" tile that smokes while [hot] is true, such as a device that is running too warm. */
@Composable
fun SmokeExample(
    hot: Boolean,
    color: Color,
) {
    Box(
        Modifier
            .size(width = 160.dp, height = 100.dp)
            .conditionalEffect(hot, ConditionalEffect.smoke(color))
            .clip(KapowShapes.control)
            .background(KapowEffectColors.coral),
        contentAlignment = Alignment.Center,
    ) {
        KapowText("Hot", color = Color.Black)
    }
}
