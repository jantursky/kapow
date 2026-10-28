package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.KapowInteraction
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText

/** A card whose halo follows [interaction]: hover it, press it or tab to it. */
@Composable
fun InteractionExample(
    interaction: KapowInteraction,
    source: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Column(
        Modifier
            .size(width = 180.dp, height = 110.dp)
            .conditionalEffect(source, interaction, ConditionalEffect.glow(KapowEffectColors.violet))
            .clip(RoundedCornerShape(18.dp))
            .background(KapowEffectColors.cobalt)
            .hoverable(source)
            .focusable(interactionSource = source)
            .clickable(interactionSource = source, indication = null) {}
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
    ) {
        KapowText("Plan", color = Color.White)
        KapowText(interaction.name, color = Color.White.copy(alpha = 0.75f))
    }
}
