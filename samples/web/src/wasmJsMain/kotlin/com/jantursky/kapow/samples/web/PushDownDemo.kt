package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.PushDownExample
import kotlin.math.roundToInt

/** The controls around [PushDownExample]. */
@Composable
internal fun PushDownDemo() {
    var pressed by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(0.95f) }
    ExampleScaffold(
        stage = { PushDownExample(pressed, scale) },
        controls = {
            PrimaryPill("Toggle", onClick = { pressed = !pressed })
            LabeledSlider("Scale", "${(scale * 100).roundToInt()} %", scale, { scale = it }, 0.5f..1f)
        },
    )
}
