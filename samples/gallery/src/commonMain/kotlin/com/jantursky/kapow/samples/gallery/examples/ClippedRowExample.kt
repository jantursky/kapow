package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.KapowParticleLayer
import com.jantursky.kapow.ParticleContent
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A list row that clips its content, with a like button at its edge. The layer lets the particles leave the row. */
@Composable
fun ClippedRowExample(
    likes: Int,
    count: Int,
) {
    val spray = ChangeEffect.spray(ParticleContent.shape(ParticleShape.Heart), count)
    KapowParticleLayer {
        Row(
            Modifier
                .width(300.dp)
                .clip(KapowShapes.stage)
                .background(KapowTheme.colors.panel)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KapowText("Morning run", color = KapowTheme.colors.title)
            Box(
                Modifier
                    .changeEffect(likes, spray)
                    .clip(CircleShape)
                    .background(KapowEffectColors.coral)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                KapowText("Like $likes", color = Color.Black)
            }
        }
    }
}
