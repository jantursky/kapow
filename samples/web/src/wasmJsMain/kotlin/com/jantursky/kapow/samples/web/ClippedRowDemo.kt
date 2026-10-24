package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.ClippedRowExample
import kotlin.math.roundToInt

/** The controls around [ClippedRowExample]. */
@Composable
internal fun ClippedRowDemo() {
    var likes by remember { mutableIntStateOf(0) }
    var particles by remember { mutableFloatStateOf(11f) }
    ExampleScaffold(
        stage = { ClippedRowExample(likes, particles.roundToInt()) },
        controls = {
            PrimaryPill("Like", onClick = { likes++ })
            LabeledSlider("Count", particles.roundToInt().toString(), particles, { particles = it }, 1f..40f)
        },
    )
}
