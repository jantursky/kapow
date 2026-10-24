package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** A coupon that is torn in along [angle], and torn off again once it is redeemed. */
@Composable
fun TearExample(
    visible: Boolean,
    angle: Float,
    toothSize: Dp,
    duration: Duration,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.tear(angle, toothSize, duration),
        exit = KapowExit.tear(angle, toothSize, duration),
    ) {
        Column(
            Modifier
                .size(width = 260.dp, height = 130.dp)
                .clip(KapowShapes.card)
                .background(KapowEffectColors.coral)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KapowText("Coupon", color = Color.Black)
            KapowText("20 % off your next order", style = KapowTheme.type.subheading, color = Color.Black)
            KapowText("Code KAPOW20", style = KapowTheme.type.numeric, color = Color.Black)
        }
    }
}
