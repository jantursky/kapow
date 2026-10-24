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
import com.jantursky.kapow.samples.gallery.examples.DissolveExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [DissolveExample]. */
@Composable
internal fun DissolveDemo() {
    var visible by remember { mutableStateOf(false) }
    var cell by remember { mutableFloatStateOf(12f) }
    var seconds by remember { mutableFloatStateOf(0.6f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { DissolveExample(visible, cell.roundToInt().dp, duration) },
        code = {
            val args = arrayOf("cellSize" to cell.roundToInt().dp.kt(), "duration" to duration.kt())
            transitionCode(call("KapowEnter.dissolve", *args), call("KapowExit.dissolve", *args))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            LabeledSlider("Cell size", "${cell.roundToInt()} dp", cell, { cell = it }, 4f..32f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
