package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.SmokeExample
import kotlin.time.Duration.Companion.seconds

/** The controls around [SmokeExample]. */
@Composable
internal fun SmokeDemo() {
    var hot by remember { mutableStateOf(true) }
    var swatch by remember { mutableStateOf(Swatch.Gray) }
    var rate by remember { mutableFloatStateOf(1f) }
    var wind by remember { mutableFloatStateOf(0f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (hot) 4.seconds else 2.seconds }, startsOn = true) { hot = !hot },
        stage = { SmokeExample(hot, swatch.color, rate, wind) },
        code = {
            val args =
                listOfNotNull(
                    "color" to swatch.color.kt(),
                    ("rate" to rate.kt()).takeIf { rate != 1f },
                    ("wind" to wind.kt()).takeIf { wind != 0f },
                )
            conditionalEffectCode(call("ConditionalEffect.smoke", *args.toTypedArray()), "hot")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { hot = !hot })
            ChoiceRow(
                "Color",
                listOf(Swatch.Gray, Swatch.Amber, Swatch.Coral, Swatch.Cobalt),
                swatch,
                { swatch = it },
                { it.name },
            )
            LabeledSlider("Rate", "${rate.oneDecimal()}x", rate, { rate = it }, 0.3f..3f)
            LabeledSlider("Wind", "${wind.toInt()} dp/s²", wind, { wind = it }, -150f..150f)
        },
    )
}
