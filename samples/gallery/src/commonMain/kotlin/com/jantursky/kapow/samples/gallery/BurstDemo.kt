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
import com.jantursky.kapow.samples.gallery.examples.BurstExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [BurstExample]. */
@Composable
internal fun BurstDemo() {
    var level by remember { mutableIntStateOf(1) }
    var swatch by remember { mutableStateOf(Swatch.Amber) }
    var spikes by remember { mutableFloatStateOf(12f) }
    ExampleScaffold(
        autoplay = Autoplay(1.6.seconds) { level++ },
        stage = { BurstExample(level, swatch.color, spikes.roundToInt()) },
        controls = {
            PrimaryPill("Level up", onClick = { level++ })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Spikes", spikes.roundToInt().toString(), spikes, { spikes = it }, 5f..24f)
        },
    )
}
