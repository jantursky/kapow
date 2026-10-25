package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText

/** A drink in an order list that fizzes while it is [fresh]ly poured. */
@Composable
fun BubblesExample(
    fresh: Boolean,
    color: Color,
    size: Dp,
    rate: Float = 1f,
    pop: Boolean = true,
    colorVariation: Float = 0f,
) {
    Column(
        Modifier
            .size(width = 160.dp, height = 110.dp)
            .conditionalEffect(
                fresh,
                ConditionalEffect.bubbles(color, size, rate = rate, pop = pop, colorVariation = colorVariation),
            ).clip(KapowShapes.control)
            .background(Brush.verticalGradient(listOf(Color(0xFF7FC4FF), KapowEffectColors.cobalt)))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
    ) {
        KapowText("Sparkling water", color = Color.White)
        KapowText(if (fresh) "Just poured" else "Gone flat", color = Color.White.copy(alpha = 0.75f))
    }
}
