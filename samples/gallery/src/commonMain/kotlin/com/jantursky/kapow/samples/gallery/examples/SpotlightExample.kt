package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A task list on its first launch: while [onboarding], everything but the add button is dimmed. */
@Composable
fun SpotlightExample(
    onboarding: Boolean,
    dim: Float,
    softness: Dp,
) {
    val spotlight = ConditionalEffect.spotlight(Color.Black.copy(alpha = dim), CircleShape, softness = softness)
    Column(Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // The scrim is drawn by the add button, so the header row draws after the list below it, and the hint
        // after the header row, to stay on top.
        Row(Modifier.fillMaxWidth().zIndex(1f), verticalAlignment = Alignment.CenterVertically) {
            KapowText("Today", style = KapowTheme.type.subheading, modifier = Modifier.weight(1f))
            Box(Modifier.size(36.dp).border(1.dp, KapowTheme.colors.hairline, CircleShape))
            Box(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(36.dp)
                    .conditionalEffect(onboarding, spotlight)
                    .clip(CircleShape)
                    .background(KapowEffectColors.cobalt),
                contentAlignment = Alignment.Center,
            ) {
                KapowText("+", style = KapowTheme.type.subheading, color = Color.White)
            }
        }
        for (task in listOf("Book flights", "Water the plants", "Call the bank")) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(KapowShapes.control)
                    .background(KapowTheme.colors.panel)
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(12.dp))
                Box(Modifier.size(14.dp).border(1.dp, KapowTheme.colors.slate, CircleShape))
                Box(Modifier.width(10.dp))
                KapowText(task)
            }
        }
        KapowText(
            "Tap + to add your first task",
            style = KapowTheme.type.label,
            color = Color.White,
            modifier = Modifier.align(Alignment.End).zIndex(2f).alpha(if (onboarding) 1f else 0f),
        )
    }
}
