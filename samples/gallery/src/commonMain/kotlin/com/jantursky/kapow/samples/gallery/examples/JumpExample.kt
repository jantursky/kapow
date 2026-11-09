package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A reminder badge that hops each time [count] changes. */
@Composable
fun JumpExample(
    count: Int,
    height: Dp,
    hops: Int,
    squash: Float,
    duration: Duration?,
) {
    // Room above, so the hop is not cut off by the stage.
    Box(Modifier.padding(top = 56.dp)) {
        Box(
            Modifier
                .size(width = 140.dp, height = 56.dp)
                .changeEffect(count, ChangeEffect.jump(height, hops, squash, duration))
                .clip(KapowShapes.pill)
                .background(KapowEffectColors.amber),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("New message", color = Color.Black)
        }
    }
}
