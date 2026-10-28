package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.jantursky.kapow.KapowTrigger
import com.jantursky.kapow.ParticleContent
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText

/** A button that sprays hearts each time [trigger] is fired, by a tap on it or by code. */
@Composable
fun TriggerExample(
    trigger: KapowTrigger,
    count: Int,
) {
    val spray = ChangeEffect.spray(ParticleContent.shape(ParticleShape.Heart), count)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .changeEffect(trigger, spray)
                .clip(CircleShape)
                .background(KapowEffectColors.coral)
                .clickable { trigger.fire() }
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            KapowText("Tap me", color = Color.Black)
        }
    }
}
