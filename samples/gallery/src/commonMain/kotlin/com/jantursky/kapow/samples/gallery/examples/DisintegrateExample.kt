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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/**
 * A notification that turns to dust when it is dismissed. With [color] unspecified the dust takes the card's own
 * colors.
 */
@Composable
fun DisintegrateExample(
    visible: Boolean,
    color: Color,
    duration: Duration,
) {
    KapowAnimatedVisibility(
        visible,
        exit = KapowExit.disintegrate(color, duration),
    ) {
        Row(
            Modifier
                .size(width = 290.dp, height = 84.dp)
                .clip(KapowShapes.card)
                .background(Brush.horizontalGradient(listOf(KapowEffectColors.coral, KapowEffectColors.violet)))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(KapowShapes.control).background(KapowEffectColors.amber))
            Column {
                KapowText("Your order has shipped", color = Color.White, maxLines = 1)
                KapowCaption("Arrives Thursday. Tap to track it.", color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}
