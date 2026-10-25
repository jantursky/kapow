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
import com.jantursky.kapow.samples.gallery.examples.HeartsExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [HeartsExample]. */
@Composable
internal fun HeartsDemo() {
    var likes by remember { mutableIntStateOf(0) }
    var swatch by remember { mutableStateOf(Swatch.Coral) }
    var count by remember { mutableFloatStateOf(6f) }
    var bigScale by remember { mutableFloatStateOf(1f) }
    ExampleScaffold(
        autoplay = Autoplay(1.8.seconds) { likes++ },
        stage = { HeartsExample(likes, swatch.color, count.roundToInt(), bigScale) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.hearts",
                    "color" to swatch.color.kt(),
                    "count" to count.roundToInt().toString(),
                    "bigHeartScale" to bigScale.kt(),
                ),
                value = "likes",
            )
        },
        controls = {
            PrimaryPill("Like", onClick = { likes++ })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Hearts", count.roundToInt().toString(), count, { count = it }, 0f..16f)
            LabeledSlider("Big heart", "${(bigScale * 100).roundToInt()} %", bigScale, { bigScale = it }, 0.5f..2f)
        },
    )
}
