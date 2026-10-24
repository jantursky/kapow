package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.SmokeExample

/** The controls around [SmokeExample]. */
@Composable
internal fun SmokeDemo() {
    var hot by remember { mutableStateOf(true) }
    var swatch by remember { mutableStateOf(Swatch.Amber) }
    ExampleScaffold(
        stage = { SmokeExample(hot, swatch.color) },
        controls = {
            PrimaryPill("Toggle", onClick = { hot = !hot })
            ChoiceRow("Color", Swatch.entries, swatch, { swatch = it }, { it.name })
        },
    )
}
