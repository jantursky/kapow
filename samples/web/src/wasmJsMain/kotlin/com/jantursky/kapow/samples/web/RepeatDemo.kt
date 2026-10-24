package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.RepeatExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** The controls around [RepeatExample]. */
@Composable
internal fun RepeatDemo() {
    var unread by remember { mutableStateOf(true) }
    var seconds by remember { mutableFloatStateOf(1f) }
    ExampleScaffold(
        stage = { RepeatExample(unread, (seconds * 1000).roundToInt().milliseconds) },
        controls = {
            PrimaryPill("Toggle", onClick = { unread = !unread })
            LabeledSlider("Every", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.1f..3f)
        },
    )
}
