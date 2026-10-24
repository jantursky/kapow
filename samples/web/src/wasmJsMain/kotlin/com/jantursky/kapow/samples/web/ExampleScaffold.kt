package com.jantursky.kapow.samples.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.EffectCard
import com.jantursky.kapow.samples.design.Stage

/** The shape of every example page: the live stage on top, its controls under it. */
@Composable
internal fun ExampleScaffold(
    stage: @Composable () -> Unit,
    controls: @Composable ColumnScope.() -> Unit,
) {
    EffectCard(Modifier.fillMaxSize()) {
        Stage(Modifier.weight(1f)) { stage() }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = controls)
    }
}
