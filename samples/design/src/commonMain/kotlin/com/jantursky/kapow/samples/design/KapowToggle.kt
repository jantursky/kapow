package com.jantursky.kapow.samples.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** A switch with its label. On is the `action` color, off is a `glass` track. */
@Composable
fun KapowToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = KapowTheme.colors
    val source = remember { MutableInteractionSource() }
    val track = if (checked) colors.action else colors.glass
    val thumb = if (checked) colors.onAction else colors.slate
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .toggleable(checked, source, indication = null, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Box(
            Modifier
                .kapowFocusRing(source, KapowShapes.pill)
                .clip(KapowShapes.pill)
                .background(track)
                .border(1.dp, colors.hairline, KapowShapes.pill)
                .size(width = 36.dp, height = 20.dp),
        ) {
            Box(
                Modifier
                    .offset(x = if (checked) 18.dp else 2.dp, y = 2.dp)
                    .size(16.dp)
                    .clip(KapowShapes.pill)
                    .background(thumb),
            )
        }
        KapowCaption(label, Modifier.padding(start = 10.dp), color = colors.ash)
    }
}
