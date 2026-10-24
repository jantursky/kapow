package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.SpotlightExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [SpotlightExample]. */
@Composable
internal fun SpotlightDemo() {
    var showTip by remember { mutableStateOf(true) }
    var dim by remember { mutableFloatStateOf(0.6f) }
    var softness by remember { mutableFloatStateOf(24f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (showTip) 3.5.seconds else 1.5.seconds }, startsOn = true) { showTip = !showTip },
        stage = { SpotlightExample(showTip, dim, softness.dp) },
        controls = {
            PrimaryPill("Toggle", onClick = { showTip = !showTip })
            LabeledSlider("Dim", "${(dim * 100).roundToInt()} %", dim, { dim = it }, 0.2f..0.9f)
            LabeledSlider("Softness", "${softness.roundToInt()} dp", softness, { softness = it }, 0f..64f)
        },
    )
}
