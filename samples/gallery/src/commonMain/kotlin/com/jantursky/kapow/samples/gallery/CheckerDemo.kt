package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.CheckerOrder
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.CheckerExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [CheckerExample]. */
@Composable
internal fun CheckerDemo() {
    var visible by remember { mutableStateOf(false) }
    var cell by remember { mutableFloatStateOf(32f) }
    var seconds by remember { mutableFloatStateOf(0.6f) }
    var order by remember { mutableStateOf(CheckerOrder.Together) }
    var seed by remember { mutableFloatStateOf(41f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { CheckerExample(visible, cell.roundToInt().dp, duration, order, seed.roundToInt()) },
        code = {
            val args =
                transitionArgs(
                    "cellSize" to cell.roundToInt().dp.kt(),
                    "duration" to duration.kt(),
                    ("order" to "CheckerOrder.${order.name}").takeIf { order != CheckerOrder.Together },
                    ("seed" to seed.roundToInt()).takeIf { order == CheckerOrder.Random },
                )
            transitionCode(call("KapowEnter.checker", *args), call("KapowExit.checker", *args))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            LabeledSlider("Cell size", "${cell.roundToInt()} dp", cell, { cell = it }, 8f..80f)
            ChoiceRow("Order", CheckerOrder.entries, order, { order = it }, { it.name })
            if (order == CheckerOrder.Random) {
                LabeledSlider("Seed", "${seed.roundToInt()}", seed, { seed = it }, 0f..99f)
            }
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
