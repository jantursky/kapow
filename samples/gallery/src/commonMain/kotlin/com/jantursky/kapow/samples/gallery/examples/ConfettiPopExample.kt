package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** An invoice row whose "Paid" badge pops confetti each time [payments] changes. */
@Composable
fun ConfettiPopExample(
    payments: Int,
    pieces: Int,
) {
    Row(
        Modifier
            .width(280.dp)
            .clip(KapowShapes.control)
            .background(KapowTheme.colors.panel)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            KapowText("Invoice #1042")
            KapowCaption("$240.00")
        }
        Box(
            Modifier
                .changeEffect(payments, ChangeEffect.confettiPop(count = pieces))
                .clip(KapowShapes.pill)
                .background(KapowEffectColors.amber)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            KapowText("Paid", style = KapowTheme.type.label, color = Color.Black)
        }
    }
}
