package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.SpeedLinesExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [SpeedLinesExample]. */
@Composable
internal fun SpeedLinesDemo() {
    var rings by remember { mutableIntStateOf(0) }
    var swatch by remember { mutableStateOf(Swatch.Violet) }
    var lines by remember { mutableFloatStateOf(12f) }
    ExampleScaffold(
        autoplay = Autoplay(1.seconds) { rings++ },
        stage = { SpeedLinesExample(rings, swatch.color, lines.roundToInt()) },
        controls = {
            PrimaryPill("Ring", onClick = { rings++ })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Lines", lines.roundToInt().toString(), lines, { lines = it }, 4f..24f)
        },
    )
}
