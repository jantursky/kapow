package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ConfettiPopExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [ConfettiPopExample]. */
@Composable
internal fun ConfettiPopDemo() {
    var payments by remember { mutableIntStateOf(0) }
    var pieces by remember { mutableFloatStateOf(28f) }
    ExampleScaffold(
        autoplay = Autoplay(2.2.seconds) { payments++ },
        stage = { ConfettiPopExample(payments, pieces.roundToInt()) },
        controls = {
            PrimaryPill("Pay", onClick = { payments++ })
            LabeledSlider("Pieces", pieces.roundToInt().toString(), pieces, { pieces = it }, 4f..80f)
        },
    )
}
