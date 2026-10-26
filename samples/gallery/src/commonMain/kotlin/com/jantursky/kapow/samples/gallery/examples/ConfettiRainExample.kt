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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText

private val Ink = Color(0xFF161616)

/** A goal card that rains confetti from its top edge while the goal is [reached]. */
@Composable
fun ConfettiRainExample(
    reached: Boolean,
    density: Float,
    size: Dp,
) {
    Column(
        Modifier
            .size(width = 200.dp, height = 110.dp)
            .conditionalEffect(reached, ConditionalEffect.confettiRain(density = density, size = size))
            .clip(KapowShapes.control)
            .background(Color(0xFFF3F4F8))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
    ) {
        KapowText("Weekly goal", color = Ink)
        KapowText(if (reached) "Reached!" else "3 of 5 workouts", color = Ink.copy(alpha = 0.6f))
    }
}
