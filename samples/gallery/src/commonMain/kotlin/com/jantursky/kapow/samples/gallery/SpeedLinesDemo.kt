package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.SpeedLineDirections
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.SpeedLinesExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [SpeedLinesExample]. */
@Composable
internal fun SpeedLinesDemo() {
    var rings by remember { mutableIntStateOf(0) }
    var swatch by remember { mutableStateOf(Swatch.Violet) }
    var lines by remember { mutableFloatStateOf(12f) }
    var directions by remember { mutableStateOf(SpeedLineDirections.All) }
    var taper by remember { mutableFloatStateOf(0f) }
    ExampleScaffold(
        autoplay = Autoplay(1.seconds) { rings++ },
        stage = { SpeedLinesExample(rings, swatch.color, lines.roundToInt(), directions, taper) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.speedLines",
                    "color" to swatch.color.kt(),
                    "count" to lines.roundToInt().toString(),
                    "taper" to "${taper.oneDecimal()}f",
                    "directions" to "SpeedLineDirections.${directions.name}",
                ),
                value = "rings",
            )
        },
        controls = {
            PrimaryPill("Ring", onClick = { rings++ })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Lines", lines.roundToInt().toString(), lines, { lines = it }, 4f..24f)
            ChoiceRow("Directions", SpeedLineDirections.entries, directions, { directions = it }, { it.name })
            LabeledSlider("Taper", taper.oneDecimal(), taper, { taper = it }, 0f..1f)
        },
    )
}
