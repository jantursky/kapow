package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.BlobExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [BlobExample]. */
@Composable
internal fun BlobDemo() {
    var visible by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf(Origin.BottomEnd) }
    var seconds by remember { mutableFloatStateOf(0.6f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { BlobExample(visible, origin.origin, duration) },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Origin", Origin.entries, origin, { origin = it }, { it.name })
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
