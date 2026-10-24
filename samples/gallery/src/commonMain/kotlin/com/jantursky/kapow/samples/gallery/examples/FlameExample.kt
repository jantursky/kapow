package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A daily streak counter that burns while the streak is [onFire]. */
@Composable
fun FlameExample(
    onFire: Boolean,
    days: Int,
    height: Dp,
) {
    Column(
        Modifier
            .width(140.dp)
            .conditionalEffect(onFire, ConditionalEffect.flame(height = height))
            .clip(KapowShapes.control)
            .background(KapowTheme.colors.panel)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        KapowText("$days", style = KapowTheme.type.headingSm, color = KapowTheme.colors.title)
        KapowCaption(if (onFire) "day streak" else "streak lost")
    }
}
