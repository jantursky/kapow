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
import com.jantursky.kapow.samples.gallery.examples.BorderBeamExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [BorderBeamExample]. */
@Composable
internal fun BorderBeamDemo() {
    var recording by remember { mutableStateOf(true) }
    var swatch by remember { mutableStateOf(Swatch.Coral) }
    var width by remember { mutableFloatStateOf(2f) }
    var lap by remember { mutableFloatStateOf(2f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (recording) 5.seconds else 1.5.seconds }, startsOn = true) { recording = !recording },
        stage = { BorderBeamExample(recording, swatch.color, width.dp, (lap * 1000).roundToInt().milliseconds) },
        controls = {
            PrimaryPill("Toggle", onClick = { recording = !recording })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Width", "${width.oneDecimal()} dp", width, { width = it }, 1f..6f)
            LabeledSlider("Lap", "${lap.oneDecimal()} s", lap, { lap = it }, 0.5f..5f)
        },
    )
}
