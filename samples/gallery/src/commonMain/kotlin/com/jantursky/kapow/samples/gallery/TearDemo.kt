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
import com.jantursky.kapow.samples.gallery.examples.TearExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [TearExample]. */
@Composable
internal fun TearDemo() {
    var visible by remember { mutableStateOf(false) }
    var angle by remember { mutableFloatStateOf(0f) }
    var tooth by remember { mutableFloatStateOf(12f) }
    var seconds by remember { mutableFloatStateOf(0.5f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { TearExample(visible, angle, tooth.roundToInt().dp, duration) },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            LabeledSlider("Angle", "${angle.roundToInt()}°", angle, { angle = it }, 0f..360f)
            LabeledSlider("Tooth size", "${tooth.roundToInt()} dp", tooth, { tooth = it }, 4f..32f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
