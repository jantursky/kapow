package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.PulseExample
import kotlin.math.roundToInt

/** The controls around [PulseExample]. */
@Composable
internal fun PulseDemo() {
    var items by remember { mutableIntStateOf(1) }
    var shape by remember { mutableStateOf(ShapeChoice.Rounded) }
    var swatch by remember { mutableStateOf(Swatch.Violet) }
    var fill by remember { mutableStateOf(false) }
    var pulses by remember { mutableFloatStateOf(1f) }
    ExampleScaffold(
        stage = { PulseExample(items, shape.shape, swatch.color, fill, pulses.roundToInt()) },
        controls = {
            PrimaryPill("Add to cart", onClick = { items++ })
            ChoiceRow("Shape", ShapeChoice.entries, shape, { shape = it }, { it.name })
            ChoiceRow("Color", Swatch.entries, swatch, { swatch = it }, { it.name })
            ChoiceRow("Style", listOf(false, true), fill, { fill = it }, { if (it) "Fill" else "Stroke" })
            LabeledSlider("Pulses", pulses.roundToInt().toString(), pulses, { pulses = it }, 1f..4f)
        },
    )
}
