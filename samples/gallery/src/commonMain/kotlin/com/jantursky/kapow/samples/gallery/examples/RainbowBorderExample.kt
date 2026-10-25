package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** The Pro plan card on a pricing page, ringed in color while it is [highlighted]. */
@Composable
fun RainbowBorderExample(
    highlighted: Boolean,
    width: Dp,
    turn: Duration,
    clockwise: Boolean = true,
    glow: Dp = 0.dp,
    alpha: Float = 1f,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .width(220.dp)
            .conditionalEffect(
                highlighted,
                ConditionalEffect.rainbowBorder(
                    width = width,
                    shape = shape,
                    duration = turn,
                    clockwise = clockwise,
                    alpha = alpha,
                    glow = glow,
                ),
            ).clip(shape)
            .background(KapowTheme.colors.panel)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KapowCaption("Pro")
        KapowText("$12 / month", style = KapowTheme.type.headingSm, color = KapowTheme.colors.title)
        KapowCaption("Unlimited projects")
    }
}
