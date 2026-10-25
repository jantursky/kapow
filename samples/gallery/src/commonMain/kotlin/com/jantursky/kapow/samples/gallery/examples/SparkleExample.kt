package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A loyalty card that twinkles with stars each time its [points] go up. */
@Composable
fun SparkleExample(
    points: Int,
    color: Color,
    count: Int,
    spread: Float = 1f,
    shape: ParticleShape = ParticleShape.Sparkle,
) {
    Column(
        Modifier
            .size(width = 240.dp, height = 140.dp)
            .changeEffect(points, ChangeEffect.sparkle(color, count, spread = spread, shape = shape))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(KapowEffectColors.violet, KapowEffectColors.cobalt)))
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        KapowText("Gold member", style = KapowTheme.type.caption, color = Color.White)
        KapowText("$points pts", style = KapowTheme.type.headingSm, color = Color.White)
    }
}
