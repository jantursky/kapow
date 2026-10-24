package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.BubblesExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The bubble colors to pick from: the default pale blue and the effect palette. */
private enum class BubbleColor(
    val color: Color,
) {
    Sky(Color(0xFF9ED8FF)),
    Amber(Swatch.Amber.color),
    Coral(Swatch.Coral.color),
    Violet(Swatch.Violet.color),
}

/** The controls around [BubblesExample]. */
@Composable
internal fun BubblesDemo() {
    var fresh by remember { mutableStateOf(true) }
    var color by remember { mutableStateOf(BubbleColor.Sky) }
    var size by remember { mutableFloatStateOf(10f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (fresh) 4.seconds else 2.seconds }, startsOn = true) { fresh = !fresh },
        stage = { BubblesExample(fresh, color.color, size.dp) },
        code = {
            conditionalEffectCode(
                call(
                    "ConditionalEffect.bubbles",
                    "color" to color.color.kt(),
                    "size" to size.dp.kt(),
                ),
                "fresh",
            )
        },
        controls = {
            PrimaryPill("Toggle", onClick = { fresh = !fresh })
            ChoiceRow("Color", BubbleColor.entries, color, { color = it }, { it.name })
            LabeledSlider("Size", "${size.roundToInt()} dp", size, { size = it }, 4f..24f)
        },
    )
}
