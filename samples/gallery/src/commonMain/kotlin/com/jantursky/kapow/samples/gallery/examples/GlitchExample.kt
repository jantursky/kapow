package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** An error toast that glitches each time [errors] changes, so a repeated failure is noticed. */
@Composable
fun GlitchExample(
    errors: Int,
    duration: Duration,
    offset: Dp,
    sliceHeight: ClosedFloatingPointRange<Float> = 0.08f..0.3f,
    jumpChance: Float = 0.5f,
) {
    Row(
        Modifier
            .changeEffect(errors, ChangeEffect.glitch(duration, offset, sliceHeight, jumpChance = jumpChance))
            .clip(KapowShapes.control)
            .background(KapowTheme.colors.panel)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(KapowShapes.pill).background(KapowEffectColors.coral),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("!", style = KapowTheme.type.label, color = Color.Black)
        }
        KapowText("Payment failed. Try again.")
    }
}
