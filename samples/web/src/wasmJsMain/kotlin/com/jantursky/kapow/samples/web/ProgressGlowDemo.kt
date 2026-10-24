package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.ProgressGlowExample
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The controls around [ProgressGlowExample]. */
@Composable
internal fun ProgressGlowDemo() {
    var progress by remember { mutableFloatStateOf(0f) }
    var radius by remember { mutableFloatStateOf(16f) }
    var shape by remember { mutableStateOf(ShapeChoice.Rounded) }
    var swatch by remember { mutableStateOf(Swatch.Violet) }
    val scope = rememberCoroutineScope()
    ExampleScaffold(
        stage = { ProgressGlowExample(progress, swatch.color, radius.dp, shape.shape) },
        controls = {
            PrimaryPill("Play", onClick = { scope.launch { sweep { progress = it } } })
            LabeledSlider("Progress", "${(progress * 100).roundToInt()} %", progress, { progress = it }, 0f..1f)
            ChoiceRow("Shape", ShapeChoice.entries, shape, { shape = it }, { it.name })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Radius", "${radius.roundToInt()} dp", radius, { radius = it }, 4f..40f)
        },
    )
}
