package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.jantursky.kapow.LocalKapowFeedback
import com.jantursky.kapow.LocalKapowMotion
import com.jantursky.kapow.multihaptic.rememberMultihapticFeedback
import com.jantursky.kapow.samples.design.KapowTheme
import com.jantursky.kapow.samples.gallery.GalleryStage

/** The Compose side of the page: the live example of the current entry, with the page's theme, motion and haptics settings. */
@Composable
internal fun Gallery() {
    KapowTheme(darkTheme = pageIsDark.value) {
        val haptics = rememberMultihapticFeedback()
        CompositionLocalProvider(
            LocalKapowMotion provides pageMotion.value,
            LocalKapowFeedback provides if (pageHaptics.value) haptics else null,
        ) {
            // The overview has no stage.
            GalleryStage(pageEntry.value, autoplay = pageAutoplay.value)
        }
    }
}
