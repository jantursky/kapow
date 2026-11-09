package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.FlickerExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [FlickerExample]. */
@Composable
internal fun FlickerDemo() {
    var visible by remember { mutableStateOf(false) }
    var seconds by remember { mutableFloatStateOf(0.8f) }
    var steps by remember { mutableFloatStateOf(8f) }
    var seed by remember { mutableFloatStateOf(61f) }
    var dim by remember { mutableFloatStateOf(0f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { FlickerExample(visible, duration, steps.roundToInt(), seed.roundToInt(), dim) },
        code = {
            fun args() =
                transitionArgs(
                    "duration" to duration.kt(),
                    ("steps" to steps.roundToInt()).takeIf { steps.roundToInt() != 8 },
                    ("seed" to seed.roundToInt()).takeIf { seed.roundToInt() != 61 },
                    ("dim" to dim.kt()).takeIf { dim != 0f },
                )
            transitionCode(enter = call("KapowEnter.flicker", *args()), exit = call("KapowExit.flicker", *args()))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            LabeledSlider("Steps", "${steps.roundToInt()}", steps, { steps = it }, 2f..30f)
            LabeledSlider("Seed", "${seed.roundToInt()}", seed, { seed = it }, 0f..200f)
            LabeledSlider("Dim", dim.oneDecimal(), dim, { dim = it }, 0f..1f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
