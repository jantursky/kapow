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
    var points by remember { mutableFloatStateOf(9f) }
    var wobble by remember { mutableFloatStateOf(0.25f) }
    var seed by remember { mutableFloatStateOf(53f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { BlobExample(visible, origin.origin, duration, points.roundToInt(), wobble, seed.roundToInt()) },
        code = {
            val args =
                transitionArgs(
                    "origin" to origin.kt(),
                    "duration" to duration.kt(),
                    ("points" to points.roundToInt()).takeIf { points.roundToInt() != 9 },
                    ("wobble" to wobble.kt()).takeIf { wobble != 0.25f },
                    ("seed" to seed.roundToInt()).takeIf { seed.roundToInt() != 53 },
                )
            transitionCode(call("KapowEnter.blob", *args), call("KapowExit.blob", *args))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Origin", Origin.entries, origin, { origin = it }, { it.name })
            LabeledSlider("Points", "${points.roundToInt()}", points, { points = it }, 4f..24f)
            LabeledSlider("Wobble", wobble.oneDecimal(), wobble, { wobble = it }, 0f..0.5f)
            LabeledSlider("Seed", "${seed.roundToInt()}", seed, { seed = it }, 0f..99f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
