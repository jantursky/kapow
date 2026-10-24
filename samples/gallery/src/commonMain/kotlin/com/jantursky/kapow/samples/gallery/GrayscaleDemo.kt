package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.GrayscaleExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [GrayscaleExample]. */
@Composable
internal fun GrayscaleDemo() {
    var disabled by remember { mutableStateOf(true) }
    var amount by remember { mutableFloatStateOf(1f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (disabled) 3.seconds else 1.5.seconds }, startsOn = true) { disabled = !disabled },
        stage = { GrayscaleExample(disabled, amount) },
        controls = {
            PrimaryPill("Toggle", onClick = { disabled = !disabled })
            LabeledSlider("Amount", "${(amount * 100).roundToInt()} %", amount, { amount = it }, 0f..1f)
        },
    )
}
