package com.jantursky.kapow.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.jantursky.kapow.KapowParticleLayer
import com.jantursky.kapow.samples.design.KapowTheme

/** Whether the examples play on their own, as the host set it on [GalleryStage]. */
internal val LocalGalleryAutoplay = compositionLocalOf { true }

/**
 * The live example of [entry] with its controls, or nothing for null (the web overview). The host provides the
 * rest around it: `KapowTheme`, `LocalKapowMotion` and, where the platform has them, `LocalKapowFeedback` haptics.
 * While [autoplay] is on, the example plays over and over until the visitor touches it.
 */
@Composable
fun GalleryStage(
    entry: Entry?,
    autoplay: Boolean,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalGalleryAutoplay provides autoplay) {
        // The canvas starts opaque white, so the stage paints the page color the cards sit on.
        KapowParticleLayer(modifier.fillMaxSize().background(KapowTheme.colors.canvas)) {
            entry?.example?.content()
        }
    }
}
