package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.GlowExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [GlowExample]. */
@Composable
internal fun GlowDemo() {
    var count by remember { mutableIntStateOf(0) }
    var radius by remember { mutableFloatStateOf(16f) }
    var shape by remember { mutableStateOf(ShapeChoice.Rounded) }
    var swatch by remember { mutableStateOf(Swatch.Violet) }
    ExampleScaffold(
        autoplay = Autoplay(2.2.seconds) { count++ },
        stage = { GlowExample(count, swatch.color, radius.dp, shape.shape) },
        controls = {
            PrimaryPill("Upgrade", onClick = { count++ })
            ChoiceRow("Shape", ShapeChoice.entries, shape, { shape = it }, { it.name })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Radius", "${radius.roundToInt()} dp", radius, { radius = it }, 4f..40f)
        },
    )
}
