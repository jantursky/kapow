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
import com.jantursky.kapow.samples.gallery.examples.MarchingAntsExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [MarchingAntsExample]. */
@Composable
internal fun MarchingAntsDemo() {
    var selected by remember { mutableStateOf(true) }
    var dash by remember { mutableFloatStateOf(6f) }
    var speed by remember { mutableFloatStateOf(24f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (selected) 4.seconds else 1.5.seconds }, startsOn = true) { selected = !selected },
        stage = { MarchingAntsExample(selected, dash.dp, speed.dp) },
        code = {
            conditionalEffectCode(
                call(
                    "ConditionalEffect.marchingAnts",
                    "shape" to "RoundedCornerShape(12.dp)",
                    "dash" to dash.dp.kt(),
                    "speed" to speed.dp.kt(),
                ),
                "selected",
            )
        },
        controls = {
            PrimaryPill("Toggle", onClick = { selected = !selected })
            LabeledSlider("Dash", "${dash.roundToInt()} dp", dash, { dash = it }, 2f..16f)
            LabeledSlider("Speed", "${speed.roundToInt()} dp/s", speed, { speed = it }, 0f..80f)
        },
    )
}
