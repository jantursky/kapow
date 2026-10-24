package com.jantursky.kapow.samples.web.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A buy button whose shine starts [delay] after [count] changes, for example once a sheet has settled. */
@Composable
fun DelayExample(
    count: Int,
    delay: Duration,
) {
    Box(
        Modifier
            .changeEffect(count, ChangeEffect.shine().delay(delay))
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(KapowEffectColors.cobalt, KapowEffectColors.violet)))
            .padding(horizontal = 40.dp, vertical = 16.dp),
    ) {
        KapowText("Buy now", color = Color.White)
    }
}
