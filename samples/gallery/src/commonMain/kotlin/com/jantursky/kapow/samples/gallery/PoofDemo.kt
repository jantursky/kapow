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
import com.jantursky.kapow.samples.gallery.examples.PoofExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The ways the puffs can drift; [degrees] is null for every way. */
private enum class Drift(
    val degrees: Float?,
) {
    Everywhere(null),
    Up(-90f),
    Down(90f),
    Left(180f),
    Right(0f),
}

/** The controls around [PoofExample]. */
@Composable
internal fun PoofDemo() {
    var visible by remember { mutableStateOf(true) }
    var swatch by remember { mutableStateOf(Swatch.Amber) }
    var seconds by remember { mutableFloatStateOf(0.4f) }
    var puffs by remember { mutableFloatStateOf(10f) }
    var size by remember { mutableFloatStateOf(28f) }
    var drift by remember { mutableStateOf(Drift.Everywhere) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    val puffSize = size.roundToInt().dp
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { PoofExample(visible, swatch.color, duration, puffs.roundToInt(), puffSize, drift.degrees) },
        code = {
            transitionCode(
                exit =
                    call(
                        "KapowExit.poof",
                        *transitionArgs(
                            "color" to swatch.color.kt(),
                            "duration" to duration.kt(),
                            ("puffCount" to puffs.roundToInt()).takeIf { puffs.roundToInt() != 10 },
                            ("puffSize" to puffSize.kt()).takeIf { size.roundToInt() != 28 },
                            drift.degrees?.let { "direction" to it.kt() },
                        ),
                    ),
            )
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Puffs", "${puffs.roundToInt()}", puffs, { puffs = it }, 1f..60f)
            LabeledSlider("Puff size", "${size.roundToInt()} dp", size, { size = it }, 8f..64f)
            ChoiceRow("Drift", Drift.entries, drift, { drift = it }, { it.name })
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
