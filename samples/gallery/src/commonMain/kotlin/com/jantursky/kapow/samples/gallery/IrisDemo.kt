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
import com.jantursky.kapow.samples.gallery.examples.IrisExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [IrisExample]. */
@Composable
internal fun IrisDemo() {
    var visible by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf(Origin.Center) }
    var seconds by remember { mutableFloatStateOf(0.5f) }
    var soft by remember { mutableFloatStateOf(0f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { IrisExample(visible, origin.origin, duration, soft.roundToInt().dp) },
        code = {
            val args =
                transitionArgs(
                    "origin" to origin.kt(),
                    "duration" to duration.kt(),
                    ("softEdge" to soft.roundToInt().dp.kt()).takeIf { soft >= 1f },
                )
            transitionCode(call("KapowEnter.iris", *args), call("KapowExit.iris", *args))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Origin", Origin.entries, origin, { origin = it }, { it.name })
            LabeledSlider("Soft edge", "${soft.roundToInt()} dp", soft, { soft = it }, 0f..60f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
