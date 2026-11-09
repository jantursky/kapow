package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.SkidEdge
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.SkidExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [SkidExample]. */
@Composable
internal fun SkidDemo() {
    var visible by remember { mutableStateOf(false) }
    var from by remember { mutableStateOf(SkidEdge.Left) }
    var to by remember { mutableStateOf(SkidEdge.Right) }
    var seconds by remember { mutableFloatStateOf(0.6f) }
    var overshoot by remember { mutableFloatStateOf(1f) }
    var lean by remember { mutableFloatStateOf(8f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { SkidExample(visible, from, to, duration, overshoot, lean) },
        code = {
            fun args(edge: SkidEdge) =
                transitionArgs(
                    "edge" to "SkidEdge.${edge.name}",
                    "duration" to duration.kt(),
                    ("overshoot" to overshoot.kt()).takeIf { overshoot != 1f },
                    ("lean" to lean.kt()).takeIf { lean != 8f },
                )
            transitionCode(
                enter = call("KapowEnter.skid", *args(from)),
                exit = call("KapowExit.skid", *args(to)),
            )
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("From", SkidEdge.entries, from, { from = it }, { it.name })
            ChoiceRow("To", SkidEdge.entries, to, { to = it }, { it.name })
            LabeledSlider("Overshoot", overshoot.oneDecimal(), overshoot, { overshoot = it }, 0f..3f)
            LabeledSlider("Lean", "${lean.roundToInt()}°", lean, { lean = it }, 0f..30f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
