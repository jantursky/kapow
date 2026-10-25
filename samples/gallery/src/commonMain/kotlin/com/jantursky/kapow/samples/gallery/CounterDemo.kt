package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.CounterMode
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.CounterExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val steps = longArrayOf(1, 7, 120, 1_850, 99_999, -40, 12, 3)

/** The controls around [CounterExample]. */
@Composable
internal fun CounterDemo() {
    var value by remember { mutableLongStateOf(1_280) }
    var step by remember { mutableLongStateOf(0) }
    var mode by remember { mutableStateOf(CounterMode.Count) }
    var millis by remember { mutableFloatStateOf(700f) }
    val duration = millis.roundToInt().milliseconds

    fun next() {
        value += steps[(step++ % steps.size).toInt()] * if (step % 5L == 0L) -1 else 1
    }
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { next() },
        stage = { CounterExample(value, mode, duration) },
        code = {
            call(
                "KapowCounter",
                "value" to "score",
                "mode" to "CounterMode.${mode.name}",
                "duration" to duration.kt(),
            ).render()
        },
        controls = {
            PrimaryPill("Change value", onClick = { next() })
            ChoiceRow("Mode", CounterMode.entries, mode, { mode = it }, { it.name })
            LabeledSlider("Duration", "${millis.roundToInt()} ms", millis, { millis = it }, 100f..2000f)
        },
    )
}
