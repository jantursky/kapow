package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.BurstExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [BurstExample]. */
@Composable
internal fun BurstDemo() {
    var level by remember { mutableIntStateOf(1) }
    var swatch by remember { mutableStateOf(Swatch.Amber) }
    var spikes by remember { mutableFloatStateOf(12f) }
    var size by remember { mutableFloatStateOf(1f) }
    var outline by remember { mutableFloatStateOf(0f) }
    var inFront by remember { mutableStateOf(false) }
    ExampleScaffold(
        autoplay = Autoplay(1.6.seconds) { level++ },
        stage = { BurstExample(level, swatch.color, spikes.roundToInt(), inFront, outline.dp, size) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.burst",
                    "color" to swatch.color.kt(),
                    "spikes" to spikes.roundToInt().toString(),
                    "sizeScale" to "${size.oneDecimal()}f",
                    "inFront" to inFront.toString(),
                    "outline" to outline.dp.kt(),
                ),
                value = "level",
            )
        },
        controls = {
            PrimaryPill("Level up", onClick = { level++ })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Spikes", spikes.roundToInt().toString(), spikes, { spikes = it }, 5f..24f)
            LabeledSlider("Size", "${size.oneDecimal()}x", size, { size = it }, 0.5f..1.6f)
            LabeledSlider("Outline", "${outline.roundToInt()} dp", outline, { outline = it }, 0f..6f)
            ChoiceRow("Layer", listOf(false, true), inFront, { inFront = it }, { if (it) "In front" else "Behind" })
        },
    )
}
