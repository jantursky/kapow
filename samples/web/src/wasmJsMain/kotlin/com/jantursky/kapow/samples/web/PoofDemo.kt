package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.PoofExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** The controls around [PoofExample]. */
@Composable
internal fun PoofDemo() {
    var visible by remember { mutableStateOf(true) }
    var swatch by remember { mutableStateOf(Swatch.Amber) }
    var seconds by remember { mutableFloatStateOf(0.4f) }
    ExampleScaffold(
        stage = { PoofExample(visible, swatch.color, (seconds * 1000).roundToInt().milliseconds) },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Color", Swatch.entries, swatch, { swatch = it }, { it.name })
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
