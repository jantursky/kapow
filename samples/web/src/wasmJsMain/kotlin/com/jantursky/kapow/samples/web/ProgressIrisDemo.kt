package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.ProgressIrisExample
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The controls around [ProgressIrisExample]. */
@Composable
internal fun ProgressIrisDemo() {
    var progress by remember { mutableFloatStateOf(0f) }
    var origin by remember { mutableStateOf(Origin.Center) }
    val scope = rememberCoroutineScope()
    ExampleScaffold(
        stage = { ProgressIrisExample(progress, origin.origin) },
        controls = {
            PrimaryPill("Play", onClick = { scope.launch { sweep { progress = it } } })
            LabeledSlider("Progress", "${(progress * 100).roundToInt()} %", progress, { progress = it }, 0f..1f)
            ChoiceRow("Origin", Origin.entries, origin, { origin = it }, { it.name })
        },
    )
}
