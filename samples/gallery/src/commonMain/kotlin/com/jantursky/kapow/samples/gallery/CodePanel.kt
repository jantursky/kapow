package com.jantursky.kapow.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.GhostPill
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowTheme
import kotlinx.coroutines.delay

/**
 * The usage snippet of the open example, with the values its controls hold now, and a Copy button. The web
 * gallery has its own version in the page; the apps that draw everything in Compose use this one.
 */
@Composable
fun CodePanel(
    code: String,
    modifier: Modifier = Modifier,
) {
    val colors = KapowTheme.colors
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    // The label goes back to "Copy" a moment after a press.
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    Column(
        modifier
            .clip(KapowShapes.control)
            .background(colors.panel)
            .border(1.dp, colors.hairline, KapowShapes.control),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            KapowCaption("Usage with the values above", color = colors.slate)
            GhostPill(
                if (copied) "Copied" else "Copy",
                onClick = {
                    clipboard.setText(AnnotatedString(code))
                    copied = true
                },
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hairline))
        SelectionContainer(Modifier.weight(1f, fill = false)) {
            BasicText(
                highlight(code, colors),
                Modifier
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp),
                style = KapowTheme.type.code.copy(color = colors.ink),
            )
        }
    }
}

private val KEYWORDS = setOf("val", "var", "fun", "if", "else", "when", "return", "true", "false", "null", "in", "is")

// Comments, strings, numbers, then words: enough for the short snippets the examples show.
private val TOKEN = Regex("""(//[^\n]*)|("(?:\\.|[^"\\])*")|(\b\d[\d_]*(?:\.\d+)?f?\b)|(\b[A-Za-z_]\w*\b)""")

/** Colors a Kotlin snippet the way the web page does: strings, numbers, keywords, types and comments. */
internal fun highlight(
    code: String,
    colors: KapowColors,
): AnnotatedString =
    buildAnnotatedString {
        var last = 0
        for (match in TOKEN.findAll(code)) {
            append(code.substring(last, match.range.first))
            val (comment, string, number, word) = match.destructured
            val color =
                when {
                    comment.isNotEmpty() -> colors.slate
                    string.isNotEmpty() -> colors.success
                    number.isNotEmpty() -> colors.warning
                    word in KEYWORDS -> colors.title
                    word.firstOrNull()?.isUpperCase() == true -> colors.title
                    else -> colors.ink
                }
            withStyle(SpanStyle(color = color)) { append(match.value) }
            last = match.range.last + 1
        }
        append(code.substring(last))
    }
