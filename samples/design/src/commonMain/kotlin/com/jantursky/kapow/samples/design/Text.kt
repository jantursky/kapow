package com.jantursky.kapow.samples.design

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun KapowText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = KapowTheme.type.body,
    color: Color = KapowTheme.colors.ink,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Secondary text for helper copy and control labels. */
@Composable
fun KapowCaption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = KapowTheme.colors.ash,
) {
    KapowText(text, modifier, style = KapowTheme.type.caption, color = color, maxLines = 1)
}

/** A timing or a count, in tabular figures so it doesn't jitter while it changes. */
@Composable
fun KapowNumeric(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = KapowTheme.colors.slate,
) {
    KapowText(text, modifier, style = KapowTheme.type.numeric, color = color, maxLines = 1)
}
