package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ShimmerExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [ShimmerExample]. */
@Composable
internal fun ShimmerDemo() {
    var loading by remember { mutableStateOf(true) }
    var angle by remember { mutableFloatStateOf(20f) }
    var sweep by remember { mutableFloatStateOf(1500f) }
    var band by remember { mutableFloatStateOf(0.4f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (loading) 6.seconds else 2.seconds }, startsOn = true) { loading = !loading },
        stage = { ShimmerExample(loading, angle, sweep.roundToInt().milliseconds, band) },
        code = {
            val args =
                listOfNotNull(
                    ("angle" to angle.kt()).takeIf { angle != 20f },
                    ("duration" to sweep.roundToInt().milliseconds.kt()).takeIf { sweep.roundToInt() != 1500 },
                    ("bandWidth" to band.kt()).takeIf { band != 0.4f },
                )
            conditionalEffectCode(call("ConditionalEffect.shimmer", *args.toTypedArray()), "loading")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { loading = !loading })
            LabeledSlider("Angle", "${angle.roundToInt()} deg", angle, { angle = it }, 0f..90f)
            LabeledSlider("Duration", "${sweep.roundToInt()} ms", sweep, { sweep = it }, 500f..3000f)
            LabeledSlider("Band width", band.oneDecimal(), band, { band = it }, 0.1f..1f)
        },
    )
}
