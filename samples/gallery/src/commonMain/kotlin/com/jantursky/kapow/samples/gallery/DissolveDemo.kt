package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.CellShape
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
    var shape by remember { mutableStateOf(CellShape.Square) }
    var softness by remember { mutableFloatStateOf(0f) }
    var seed by remember { mutableFloatStateOf(31f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = {
            DissolveExample(visible, cell.roundToInt().dp, duration, shape, softness, seed.roundToInt())
        },
        code = {
            val args =
                transitionArgs(
                    "cellSize" to cell.roundToInt().dp.kt(),
                    "duration" to duration.kt(),
                    ("cellShape" to "CellShape.${shape.name}").takeIf { shape != CellShape.Square },
                    ("softness" to softness.kt()).takeIf { softness > 0f },
                    ("seed" to seed.roundToInt()).takeIf { seed.roundToInt() != 31 },
                )
            transitionCode(call("KapowEnter.dissolve", *args), call("KapowExit.dissolve", *args))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            LabeledSlider("Cell size", "${cell.roundToInt()} dp", cell, { cell = it }, 4f..32f)
            ChoiceRow("Cell shape", CellShape.entries, shape, { shape = it }, { it.name })
            LabeledSlider("Softness", softness.oneDecimal(), softness, { softness = it }, 0f..1f)
            LabeledSlider("Seed", "${seed.roundToInt()}", seed, { seed = it }, 0f..99f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
