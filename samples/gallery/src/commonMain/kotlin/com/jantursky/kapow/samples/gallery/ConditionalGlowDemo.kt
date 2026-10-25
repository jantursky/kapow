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
import com.jantursky.kapow.samples.gallery.examples.ConditionalGlowExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [ConditionalGlowExample]. */
@Composable
internal fun ConditionalGlowDemo() {
    var selected by remember { mutableStateOf(true) }
    var radius by remember { mutableFloatStateOf(16f) }
    var shape by remember { mutableStateOf(ShapeChoice.Rounded) }
    var swatch by remember { mutableStateOf(Swatch.Violet) }
    var strength by remember { mutableFloatStateOf(0.7f) }
    var breathe by remember { mutableFloatStateOf(0f) }
    var inner by remember { mutableFloatStateOf(0.25f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (selected) 2.5.seconds else 1.5.seconds }, startsOn = true) { selected = !selected },
        stage = { ConditionalGlowExample(selected, swatch.color, radius.dp, shape.shape, strength, breathe, inner) },
        code = {
            val args =
                listOfNotNull(
                    "color" to swatch.color.kt(),
                    "radius" to radius.dp.kt(),
                    "shape" to shape.source(),
                    ("strength" to strength.kt()).takeIf { strength != 0.7f },
                    ("breathe" to breathe.kt()).takeIf { breathe != 0f },
                    ("brighten" to inner.kt()).takeIf { inner != 0.25f },
                )
            conditionalEffectCode(call("ConditionalEffect.glow", *args.toTypedArray()), "selected")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { selected = !selected })
            ChoiceRow("Shape", ShapeChoice.entries, shape, { shape = it }, { it.name })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Radius", "${radius.roundToInt()} dp", radius, { radius = it }, 4f..40f)
            LabeledSlider("Strength", strength.oneDecimal(), strength, { strength = it }, 0.1f..1.5f)
            LabeledSlider("Breathe", "${(breathe * 100).roundToInt()} %", breathe, { breathe = it }, 0f..1f)
            LabeledSlider("Inner glow", inner.oneDecimal(), inner, { inner = it }, 0f..1f)
        },
    )
}

/** The shape as the code a person would write. */
private fun ShapeChoice.source(): String =
    when (this) {
        ShapeChoice.Rectangle -> "RectangleShape"
        ShapeChoice.Rounded -> "RoundedCornerShape(16.dp)"
        ShapeChoice.Circle -> "CircleShape"
    }
