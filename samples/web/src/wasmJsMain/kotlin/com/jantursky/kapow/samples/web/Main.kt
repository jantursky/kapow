package com.jantursky.kapow.samples.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.jantursky.kapow.samples.design.KapowTheme
import kotlinx.browser.document

/**
 * Mounts the frame meter, the stage and the overview grid into the elements index.html sets aside, and fills in
 * the page around them.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    renderEntryLists()
    watchPage()
    showPage(pageEntry.value)
    showAutoplay(pageAutoplay.value)
    document.getElementById("frame-meter")?.let { element ->
        ComposeViewport(element) { KapowTheme(darkTheme = pageIsDark.value) { FrameMeter() } }
    }
    document.getElementById("stage")?.let { element ->
        ComposeViewport(element) { Gallery() }
    }
    document.getElementById("grid-canvas")?.let { element ->
        ComposeViewport(element) { OverviewGrid() }
        watchGrid()
    }
}
