package com.jantursky.kapow.samples.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowMotion
import com.jantursky.kapow.LocalKapowMotion
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import com.jantursky.kapow.samples.gallery.CodePanel
import com.jantursky.kapow.samples.gallery.Entry
import com.jantursky.kapow.samples.gallery.GalleryStage

/**
 * The whole window: the top bar with the theme, reduced-motion and autoplay settings, the sidebar of every
 * catalog entry, and the selected entry's live example, or the overview of every entry while none is selected.
 * There are no haptics on desktop.
 */
@Composable
fun DesktopGallery(
    initialEntry: Entry? = null,
    initialDark: Boolean = true,
) {
    var entry by remember { mutableStateOf(initialEntry) }
    var dark by remember { mutableStateOf(initialDark) }
    var motion by remember { mutableStateOf(KapowMotion.System) }
    var autoplay by remember { mutableStateOf(true) }
    KapowTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalKapowMotion provides motion) {
            Column(Modifier.fillMaxSize().background(KapowTheme.colors.canvas)) {
                TopBar(
                    dark = dark,
                    onDarkChange = { dark = it },
                    motion = motion,
                    onMotionChange = { motion = it },
                    autoplay = autoplay,
                    onAutoplayChange = { autoplay = it },
                )
                Row(Modifier.weight(1f)) {
                    Sidebar(entry, onSelect = { entry = it }, Modifier.fillMaxHeight())
                    val page = Modifier.weight(1f).fillMaxHeight()
                    when (val selected = entry) {
                        null -> Overview(autoplay, onOpen = { entry = it }, page)
                        else -> ExamplePage(selected, autoplay, page)
                    }
                }
            }
        }
    }
}

/** The group, title and summary of [entry] over its live example. */
@Composable
private fun ExamplePage(
    entry: Entry,
    autoplay: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = KapowTheme.colors
    Column(modifier.padding(horizontal = 32.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        KapowCaption(entry.group.title, color = colors.slate)
        KapowText(entry.title, style = KapowTheme.type.headingSm, color = colors.title)
        KapowText(entry.summary, color = colors.ash)
        Spacer(Modifier.height(16.dp))
        var code by remember(entry) { mutableStateOf("") }
        GalleryStage(entry, autoplay, Modifier.weight(1f), onCode = { code = it })
        if (code.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            CodePanel(code, Modifier.heightIn(max = 190.dp))
        }
    }
}
