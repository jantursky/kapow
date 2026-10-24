package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.PulseExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [PulseExample]. */
@Composable
internal fun PulseDemo() {
    var items by remember { mutableIntStateOf(1) }
    var shape by remember { mutableStateOf(ShapeChoice.Rounded) }
    var swatch by remember { mutableStateOf(Swatch.Amber) }
    var fill by remember { mutableStateOf(false) }
    var pulses by remember { mutableFloatStateOf(1f) }
    ExampleScaffold(
        autoplay = Autoplay(2.seconds + 0.5.seconds * pulses.roundToInt()) { items++ },
        stage = { PulseExample(items, shape.shape, swatch.color, fill, pulses.roundToInt()) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.pulse",
                    "shape" to shape.kt(),
                    "color" to swatch.color.kt(),
                    "style" to if (fill) "PulseStyle.Fill" else "PulseStyle.Stroke()",
                    "count" to pulses.roundToInt().toString(),
                ),
                value = "items",
            )
        },
        controls = {
            PrimaryPill("Add to cart", onClick = { items++ })
            ChoiceRow("Shape", ShapeChoice.entries, shape, { shape = it }, { it.name })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            ChoiceRow("Style", listOf(false, true), fill, { fill = it }, { if (it) "Fill" else "Stroke" })
            LabeledSlider("Pulses", pulses.roundToInt().toString(), pulses, { pulses = it }, 1f..4f)
        },
    )
}

/** The shape as the source a user would write. */
private fun ShapeChoice.kt(): String =
    when (this) {
        ShapeChoice.Rectangle -> "RectangleShape"
        ShapeChoice.Rounded -> "RoundedCornerShape(16.dp)"
        ShapeChoice.Circle -> "CircleShape"
    }
