package com.jantursky.kapow.samples.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
private fun Pill(
    text: String,
    onClick: () -> Unit,
    fill: Color?,
    textColor: Color,
    modifier: Modifier,
) {
    val colors = KapowTheme.colors
    val source = remember { MutableInteractionSource() }
    KapowText(
        text = text,
        style = KapowTheme.type.label,
        color = textColor,
        maxLines = 1,
        modifier =
            modifier
                .kapowFocusRing(source, KapowShapes.pill)
                .clip(KapowShapes.pill)
                .then(
                    if (fill != null) {
                        Modifier.background(fill)
                    } else {
                        Modifier.border(1.dp, colors.hairline, KapowShapes.pill)
                    },
                ).clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = if (fill != null) 8.dp else 6.dp),
    )
}

/** The one filled button of a view: "Fire", "Toggle", "Show". */
@Composable
fun PrimaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KapowTheme.colors
    Pill(text, onClick, fill = colors.action, textColor = colors.onAction, modifier = modifier)
}

@Composable
fun GhostPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Pill(text, onClick, fill = null, textColor = KapowTheme.colors.ink, modifier = modifier)
}
