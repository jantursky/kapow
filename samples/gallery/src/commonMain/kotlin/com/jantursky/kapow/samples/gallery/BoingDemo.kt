package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.BoingExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [BoingExample]. */
@Composable
internal fun BoingDemo() {
    var visible by remember { mutableStateOf(false) }
    var seconds by remember { mutableFloatStateOf(0.7f) }
    var overshoot by remember { mutableFloatStateOf(1f) }
    var squash by remember { mutableFloatStateOf(0.5f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { BoingExample(visible, duration, overshoot, squash) },
        code = {
            fun args() =
                transitionArgs(
                    "duration" to duration.kt(),
                    ("overshoot" to overshoot.kt()).takeIf { overshoot != 1f },
                    ("squash" to squash.kt()).takeIf { squash != 0.5f },
                )
            transitionCode(enter = call("KapowEnter.boing", *args()), exit = call("KapowExit.boing", *args()))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            LabeledSlider("Overshoot", overshoot.oneDecimal(), overshoot, { overshoot = it }, 0f..3f)
            LabeledSlider("Squash", squash.oneDecimal(), squash, { squash = it }, 0f..1f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
