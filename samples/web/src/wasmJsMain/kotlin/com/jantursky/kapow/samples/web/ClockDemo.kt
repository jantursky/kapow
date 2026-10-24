package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.ClockExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** The controls around [ClockExample]. */
@Composable
internal fun ClockDemo() {
    var visible by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf(Origin.Center) }
    var seconds by remember { mutableFloatStateOf(0.5f) }
    ExampleScaffold(
        stage = { ClockExample(visible, origin.origin, (seconds * 1000).roundToInt().milliseconds) },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Origin", Origin.entries, origin, { origin = it }, { it.name })
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
