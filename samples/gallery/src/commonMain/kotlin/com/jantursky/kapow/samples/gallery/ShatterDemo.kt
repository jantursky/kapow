package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ShatterExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [ShatterExample]. */
@Composable
internal fun ShatterDemo() {
    var visible by remember { mutableStateOf(true) }
    var shards by remember { mutableFloatStateOf(24f) }
    var seconds by remember { mutableFloatStateOf(0.9f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { ShatterExample(visible, shards.roundToInt(), duration) },
        code = {
            val exit = call("KapowExit.shatter", "shards" to shards.roundToInt(), "duration" to duration.kt())
            transitionCode(exit = exit)
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            LabeledSlider("Shards", "${shards.roundToInt()}", shards, { shards = it }, 6f..80f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.3f..3f)
        },
    )
}
