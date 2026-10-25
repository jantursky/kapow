package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** A to-do row that sends an ink ripple across itself from [origin] each time it is ticked or unticked. */
@Composable
fun RippleExample(
    done: Boolean,
    color: Color,
    origin: TransformOrigin,
    duration: Duration,
    rings: Int = 1,
    ringWidth: Dp = Dp.Unspecified,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .size(width = 260.dp, height = 64.dp)
            .changeEffect(
                done,
                ChangeEffect.ripple(
                    color.copy(alpha = 0.35f),
                    shape,
                    origin,
                    duration,
                    rings = rings,
                    ringWidth = ringWidth,
                ),
            ).clip(shape)
            .background(KapowTheme.colors.panel)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .border(2.dp, color, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(if (done) color else Color.Transparent),
        )
        KapowText(
            "Water the plants",
            color = if (done) KapowTheme.colors.slate else KapowTheme.colors.ink,
        )
    }
}
