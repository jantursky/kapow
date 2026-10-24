package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.DelayExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [DelayExample]. */
@Composable
internal fun DelayDemo() {
    var count by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableFloatStateOf(0.5f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 2.seconds) { count++ },
        stage = { DelayExample(count, duration) },
        code = {
            call(
                "Modifier.changeEffect",
                "" to "count",
                "" to "ChangeEffect.shine().delay(${duration.kt()})",
            ).render()
        },
        controls = {
            PrimaryPill("Fire", onClick = { count++ })
            LabeledSlider("Delay", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0f..2f)
        },
    )
}
