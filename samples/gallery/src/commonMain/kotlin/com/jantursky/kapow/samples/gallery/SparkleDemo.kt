package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.SparkleExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** Star colors that read on the card's violet to cobalt gradient. */
private enum class StarColor(
    val color: Color,
) {
    White(Color.White),
    Amber(KapowEffectColors.amber),
    Coral(KapowEffectColors.coral),
}

/** The controls around [SparkleExample]. */
@Composable
internal fun SparkleDemo() {
    var points by remember { mutableIntStateOf(1200) }
    var star by remember { mutableStateOf(StarColor.White) }
    var count by remember { mutableFloatStateOf(6f) }
    ExampleScaffold(
        autoplay = Autoplay(1.6.seconds) { points += 50 },
        stage = { SparkleExample(points, star.color, count.roundToInt()) },
        code = {
            changeEffectCode(
                call("ChangeEffect.sparkle", "color" to star.color.kt(), "count" to count.roundToInt().toString()),
                value = "points",
            )
        },
        controls = {
            PrimaryPill("Add points", onClick = { points += 50 })
            ChoiceRow("Color", StarColor.entries, star, { star = it }, { it.name })
            LabeledSlider("Stars", count.roundToInt().toString(), count, { count = it }, 1f..16f)
        },
    )
}
