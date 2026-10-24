package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.StampExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [StampExample]. */
@Composable
internal fun StampDemo() {
    var approvals by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1.3f) }
    ExampleScaffold(
        autoplay = Autoplay(1.8.seconds) { approvals++ },
        stage = { StampExample(approvals, scale) },
        controls = {
            PrimaryPill("Approve", onClick = { approvals++ })
            LabeledSlider("Scale", "${(scale * 100).roundToInt()} %", scale, { scale = it }, 1f..2f)
        },
    )
}
