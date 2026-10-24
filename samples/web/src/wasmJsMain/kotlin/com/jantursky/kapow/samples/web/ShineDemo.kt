package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.ShineExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** The controls around [ShineExample]. */
@Composable
internal fun ShineDemo() {
    var count by remember { mutableIntStateOf(0) }
    var angle by remember { mutableFloatStateOf(45f) }
    var seconds by remember { mutableFloatStateOf(1f) }
    ExampleScaffold(
        stage = { ShineExample(count, angle, (seconds * 1000).roundToInt().milliseconds) },
        controls = {
            PrimaryPill("Fire", onClick = { count++ })
            LabeledSlider("Angle", "${angle.roundToInt()}°", angle, { angle = it }, 0f..360f)
            LabeledSlider("Duration", "${(seconds * 10).roundToInt() / 10f} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
