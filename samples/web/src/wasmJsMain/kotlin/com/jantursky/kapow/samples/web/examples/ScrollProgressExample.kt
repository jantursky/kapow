package com.jantursky.kapow.samples.web.examples

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ProgressEffect
import com.jantursky.kapow.progressEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/**
 * A header whose effect follows how far the list below it has scrolled. The progress lambda reads the scroll
 * offset while drawing, so scrolling redraws the header without recomposing it.
 */
@Composable
fun ScrollProgressExample(
    scrollState: ScrollState,
    glow: Boolean,
) {
    val effect =
        if (glow) {
            ProgressEffect.glow(
                KapowEffectColors.violet,
                shape = KapowShapes.control,
            )
        } else {
            ProgressEffect.shine()
        }
    Column(Modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .progressEffect(effect) { scrollState.value.toFloat() / scrollState.maxValue.coerceAtLeast(1) }
                .clip(KapowShapes.control)
                .background(KapowEffectColors.violet),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("Chapter 3", color = Color.White)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(KapowShapes.control)
                .background(KapowTheme.colors.panel)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(12) { KapowText("Paragraph ${it + 1}. Scroll the list and watch the header.") }
        }
    }
}
