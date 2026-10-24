package com.jantursky.kapow.samples.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.jantursky.kapow.KapowParticleLayer
import com.jantursky.kapow.LocalKapowMotion
import com.jantursky.kapow.samples.design.KapowTheme

/** The Compose side of the page: the live example of the current entry, with the page's theme and motion setting. */
@Composable
internal fun Gallery() {
    KapowTheme(darkTheme = pageIsDark.value) {
        CompositionLocalProvider(LocalKapowMotion provides pageMotion.value) {
            // The canvas starts opaque white, so the stage paints the page color the cards sit on.
            KapowParticleLayer(Modifier.fillMaxSize().background(KapowTheme.colors.canvas)) {
                val entry = pageEntry.value
                // The overview has no stage.
                entry?.example?.content()
            }
        }
    }
}
