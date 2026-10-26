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
import com.jantursky.kapow.samples.gallery.examples.BreatheExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [BreatheExample]. */
@Composable
internal fun BreatheDemo() {
    var listening by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1.04f) }
    var float by remember { mutableFloatStateOf(0f) }
    var period by remember { mutableFloatStateOf(3000f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (listening) 7.seconds else 2.seconds }, startsOn = true) { listening = !listening },
        stage = { BreatheExample(listening, scale, float.dp, period.roundToInt().milliseconds) },
        code = {
            val args =
                listOfNotNull(
                    "scale" to scale.kt(),
                    ("float" to float.dp.kt()).takeIf { float > 0f },
                    ("duration" to period.roundToInt().milliseconds.kt()).takeIf { period.roundToInt() != 3000 },
                )
            conditionalEffectCode(call("ConditionalEffect.breathe", *args.toTypedArray()), "listening")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { listening = !listening })
            LabeledSlider("Scale", scale.kt(), scale, { scale = it }, 1f..1.2f)
            LabeledSlider("Float", "${float.roundToInt()} dp", float, { float = it }, 0f..16f)
            LabeledSlider("Duration", "${period.roundToInt()} ms", period, { period = it }, 1000f..6000f)
        },
    )
}
