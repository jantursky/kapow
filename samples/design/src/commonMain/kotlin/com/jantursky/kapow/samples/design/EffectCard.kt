package com.jantursky.kapow.samples.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** A frosted `glass` card with the 24 radius. No border, no shadow. */
@Composable
fun EffectCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .clip(KapowShapes.card)
                .background(KapowTheme.colors.glass)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}

/** The `stage` behind a live effect. It does not clip, so particles can leave it. */
@Composable
fun Stage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(KapowTheme.colors.stage, KapowShapes.stage)
                .border(1.dp, KapowTheme.colors.hairline, KapowShapes.stage),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
