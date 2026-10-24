package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.BlindsOrientation
import com.jantursky.kapow.samples.design.KapowToggle
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.BlindsExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [BlindsExample]. */
@Composable
internal fun BlindsDemo() {
    var visible by remember { mutableStateOf(false) }
    var width by remember { mutableFloatStateOf(10f) }
    var orientation by remember { mutableStateOf(BlindsOrientation.Horizontal) }
    var staggered by remember { mutableStateOf(false) }
    var seconds by remember { mutableFloatStateOf(0.5f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = {
            BlindsExample(
                visible,
                width.dp,
                orientation,
                staggered,
                duration,
            )
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Direction", BlindsOrientation.entries, orientation, { orientation = it }, { it.name })
            LabeledSlider("Slat width", "${width.roundToInt()} dp", width, { width = it }, 5f..40f)
            KapowToggle(staggered, { staggered = it }, "Staggered")
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
