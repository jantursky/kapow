package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.PingExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [PingExample]. */
@Composable
internal fun PingDemo() {
    var live by remember { mutableStateOf(true) }
    var shape by remember { mutableStateOf(ShapeChoice.Circle) }
    var swatch by remember { mutableStateOf(Swatch.Coral) }
    var rings by remember { mutableFloatStateOf(2f) }
    var scale by remember { mutableFloatStateOf(1.6f) }
    var period by remember { mutableFloatStateOf(1600f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (live) 6.seconds else 3.seconds }, startsOn = true) { live = !live },
        stage = {
            PingExample(live, swatch.color, shape.shape, rings.roundToInt(), scale, period.roundToInt().milliseconds)
        },
        code = {
            val args =
                listOfNotNull(
                    "color" to swatch.color.kt(),
                    "shape" to shape.kt(),
                    ("rings" to rings.roundToInt().toString()).takeIf { rings.roundToInt() != 2 },
                    ("scale" to scale.kt()).takeIf { scale != 1.6f },
                    ("duration" to period.roundToInt().milliseconds.kt()).takeIf { period.roundToInt() != 1600 },
                )
            conditionalEffectCode(call("ConditionalEffect.ping", *args.toTypedArray()), "live")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { live = !live })
            ChoiceRow("Shape", ShapeChoice.entries, shape, { shape = it }, { it.name })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Rings", rings.roundToInt().toString(), rings, { rings = it }, 1f..4f)
            LabeledSlider("Scale", scale.kt(), scale, { scale = it }, 1.2f..2.5f)
            LabeledSlider("Duration", "${period.roundToInt()} ms", period, { period = it }, 600f..4000f)
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
