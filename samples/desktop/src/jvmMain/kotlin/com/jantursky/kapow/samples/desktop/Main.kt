package com.jantursky.kapow.samples.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** Opens the gallery window. */
fun main() {
    // The name macOS shows in the menu bar instead of the main class.
    System.setProperty("apple.awt.application.name", "Kapow gallery")
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kapow gallery",
            state = rememberWindowState(width = 1200.dp, height = 820.dp),
        ) {
            window.minimumSize = java.awt.Dimension(880, 600)
            DesktopGallery()
        }
    }
}
