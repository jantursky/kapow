package com.jantursky.kapow.samples.web.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** An unread dot that pulses every [every] while [unread] is true. */
@Composable
fun RepeatExample(
    unread: Boolean,
    every: Duration,
) {
    val pulse = ChangeEffect.pulse(shape = CircleShape, color = KapowEffectColors.coral)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        KapowText("Inbox", style = KapowTheme.type.subheading)
        Box(
            Modifier
                .size(14.dp)
                .conditionalEffect(unread, ConditionalEffect.repeat(pulse, every))
                .clip(CircleShape)
                .background(if (unread) KapowEffectColors.coral else KapowTheme.colors.hairline),
        )
    }
}
