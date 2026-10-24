package com.jantursky.kapow.samples.web.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.ParticleContent
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText

/** A like button that sprays particles each time [likes] changes. */
@Composable
fun SprayExample(
    likes: Int,
    shape: ParticleShape,
    count: Int,
) {
    val spray = ChangeEffect.spray(ParticleContent.shape(shape), count)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .changeEffect(likes, spray)
                .clip(CircleShape)
                .background(KapowEffectColors.coral)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            KapowText("Like", color = Color.Black)
        }
        KapowText("$likes")
    }
}
