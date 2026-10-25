package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.BlindsOrientation
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.CurtainExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [CurtainExample]. */
@Composable
internal fun CurtainDemo() {
    var visible by remember { mutableStateOf(false) }
    var orientation by remember { mutableStateOf(BlindsOrientation.Vertical) }
    var seconds by remember { mutableFloatStateOf(0.6f) }
    var split by remember { mutableFloatStateOf(0.5f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { CurtainExample(visible, orientation, duration, split) },
        code = {
            val args =
                transitionArgs(
                    "orientation" to orientation.kt(),
                    "duration" to duration.kt(),
                    ("split" to split.kt()).takeIf { split != 0.5f },
                )
            transitionCode(call("KapowEnter.curtain", *args), call("KapowExit.curtain", *args))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Seam", BlindsOrientation.entries, orientation, { orientation = it }, { it.name })
            LabeledSlider("Split", "${(split * 100).roundToInt()} %", split, { split = it }, 0f..1f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
