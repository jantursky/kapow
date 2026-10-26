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
import com.jantursky.kapow.samples.gallery.examples.ConfettiRainExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [ConfettiRainExample]. */
@Composable
internal fun ConfettiRainDemo() {
    var reached by remember { mutableStateOf(true) }
    var density by remember { mutableFloatStateOf(24f) }
    var size by remember { mutableFloatStateOf(8f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (reached) 4.seconds else 3.seconds }, startsOn = true) { reached = !reached },
        stage = { ConfettiRainExample(reached, density, size.dp) },
        code = {
            val args =
                listOfNotNull(
                    ("density" to density.roundToInt().toFloat().kt()).takeIf { density.roundToInt() != 24 },
                    ("size" to size.roundToInt().dp.kt()).takeIf { size.roundToInt() != 8 },
                )
            conditionalEffectCode(call("ConditionalEffect.confettiRain", *args.toTypedArray()), "reached")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { reached = !reached })
            LabeledSlider("Density", "${density.roundToInt()} per s", density, { density = it }, 4f..120f)
            LabeledSlider("Size", "${size.roundToInt()} dp", size, { size = it }, 4f..16f)
        },
    )
}
