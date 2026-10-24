package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A menu item with a "New" badge that sparkles while the feature [isNew] to the person. */
@Composable
fun ConditionalSparklesExample(
    isNew: Boolean,
    count: Int,
    size: Dp,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        KapowText("Smart folders", style = KapowTheme.type.subheading)
        KapowText(
            "New",
            style = KapowTheme.type.label,
            color = Color.Black,
            modifier =
                Modifier
                    .conditionalEffect(isNew, ConditionalEffect.sparkles(count = count, size = size))
                    .clip(KapowShapes.pill)
                    .background(if (isNew) KapowEffectColors.amber else KapowTheme.colors.hairline)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
