package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText

/** A button that is pressed in while [pressed] is true, as it would be under a finger. */
@Composable
fun PushDownExample(
    pressed: Boolean,
    scale: Float,
    damping: Float = 0.6f,
    dim: Float = 0f,
    translation: Dp = 0.dp,
) {
    Box(
        Modifier
            .conditionalEffect(
                pressed,
                ConditionalEffect.pushDown(scale, damping = damping, dim = dim, translation = translation),
            ).clip(KapowShapes.pill)
            .background(KapowEffectColors.cobalt)
            .padding(horizontal = 48.dp, vertical = 20.dp),
    ) {
        KapowText("Checkout", color = Color.White)
    }
}
