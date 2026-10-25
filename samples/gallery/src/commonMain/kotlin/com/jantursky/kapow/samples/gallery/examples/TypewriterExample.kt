package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowTypewriter
import com.jantursky.kapow.TypewriterCursor
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** A line that types itself. Picking another [text] restarts it; the box keeps the size of the full line. */
@Composable
fun TypewriterExample(
    text: String,
    charsPerSecond: Float,
    cursor: Boolean,
    punctuationPause: Duration = 0.milliseconds,
) {
    KapowTypewriter(
        text = text,
        modifier = Modifier.width(260.dp),
        style = KapowTheme.type.body.copy(color = KapowTheme.colors.ink),
        charsPerSecond = charsPerSecond,
        cursor = if (cursor) TypewriterCursor.bar(color = KapowTheme.colors.action) else null,
        punctuationPause = punctuationPause,
    )
}
