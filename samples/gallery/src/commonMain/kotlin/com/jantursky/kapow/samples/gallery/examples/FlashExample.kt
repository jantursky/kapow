package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** A PIN entry whose digit boxes flash in [color] each time [attempts] counts another wrong code. */
@Composable
fun FlashExample(
    attempts: Int,
    color: Color,
    duration: Duration,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // The flash tints only what the row draws: the boxes and digits, not the gaps between them.
        Row(
            Modifier.changeEffect(attempts, ChangeEffect.flash(color, duration)),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (digit in listOf("4", "7", "1", "9")) {
                Box(
                    Modifier
                        .size(width = 48.dp, height = 56.dp)
                        .background(KapowTheme.colors.panel, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    KapowText(digit, style = KapowTheme.type.headingSm, color = KapowTheme.colors.title)
                }
            }
        }
        KapowCaption(if (attempts == 0) "Enter your PIN" else "Wrong PIN. Attempts: $attempts")
    }
}
