package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.jantursky.kapow.samples.design.KapowToggle
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.GrayscaleExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The brown tint of the sepia look. */
private val SEPIA = Color(0xFFB08850)

/** The controls around [GrayscaleExample]. */
@Composable
internal fun GrayscaleDemo() {
    var disabled by remember { mutableStateOf(true) }
    var amount by remember { mutableFloatStateOf(1f) }
    var dim by remember { mutableFloatStateOf(0f) }
    var sepia by remember { mutableStateOf(false) }
    ExampleScaffold(
        autoplay = Autoplay({ if (disabled) 3.seconds else 1.5.seconds }, startsOn = true) { disabled = !disabled },
        stage = { GrayscaleExample(disabled, amount, if (sepia) SEPIA else null, dim) },
        code = {
            val args =
                listOfNotNull(
                    "amount" to amount.kt(),
                    ("tint" to SEPIA.kt()).takeIf { sepia },
                    ("dim" to dim.kt()).takeIf { dim != 0f },
                )
            conditionalEffectCode(call("ConditionalEffect.grayscale", *args.toTypedArray()), "disabled")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { disabled = !disabled })
            LabeledSlider("Amount", "${(amount * 100).roundToInt()} %", amount, { amount = it }, 0f..1f)
            LabeledSlider("Dim", "${(dim * 100).roundToInt()} %", dim, { dim = it }, 0f..0.8f)
            KapowToggle(sepia, { sepia = it }, "Sepia tint")
        },
    )
}
