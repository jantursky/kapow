package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.SpinAxis
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.Rotate3DExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [Rotate3DExample]. */
@Composable
internal fun Rotate3DDemo() {
    var visible by remember { mutableStateOf(false) }
    var axis by remember { mutableStateOf(SpinAxis.Y) }
    var seconds by remember { mutableFloatStateOf(0.6f) }
    var degrees by remember { mutableFloatStateOf(90f) }
    var shading by remember { mutableFloatStateOf(0.35f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { Rotate3DExample(visible, axis, duration, degrees, shading) },
        code = {
            fun args() =
                transitionArgs(
                    "axis" to "SpinAxis.${axis.name}",
                    "duration" to duration.kt(),
                    ("degrees" to degrees.kt()).takeIf { degrees != 90f },
                    ("shading" to shading.kt()).takeIf { shading != 0.35f },
                )
            transitionCode(enter = call("KapowEnter.rotate3D", *args()), exit = call("KapowExit.rotate3D", *args()))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Axis", listOf(SpinAxis.X, SpinAxis.Y), axis, { axis = it }, { it.name })
            LabeledSlider("Start angle", "${degrees.roundToInt()}°", degrees, { degrees = it }, 10f..180f)
            LabeledSlider("Shading", shading.oneDecimal(), shading, { shading = it }, 0f..1f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
