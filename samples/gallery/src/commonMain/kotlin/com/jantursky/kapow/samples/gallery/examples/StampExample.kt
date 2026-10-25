package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** An expense report that gets an "Approved" stamp each time [approvals] changes. */
@Composable
fun StampExample(
    approvals: Int,
    scale: Float,
    tilt: Float = 0f,
    dustCount: Int = 5,
) {
    val colors = KapowTheme.colors
    Box(
        Modifier
            .size(width = 240.dp, height = 150.dp)
            .clip(KapowShapes.card)
            .background(colors.panel)
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KapowText("Expense report", style = KapowTheme.type.label)
            for (width in listOf(1f, 0.8f, 0.55f)) {
                Box(
                    Modifier
                        .fillMaxWidth(width)
                        .height(6.dp)
                        .clip(KapowShapes.pill)
                        .background(colors.hairline),
                )
            }
        }
        val stampShape = RoundedCornerShape(6.dp)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .changeEffect(approvals, ChangeEffect.stamp(scale, tilt = tilt, dustCount = dustCount))
                .rotate(-8f)
                .border(2.dp, KapowEffectColors.coral, stampShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            KapowText("APPROVED", style = KapowTheme.type.label, color = KapowEffectColors.coral)
        }
    }
}
