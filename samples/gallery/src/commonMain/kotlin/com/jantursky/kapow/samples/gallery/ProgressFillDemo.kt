package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ProgressFillExample
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [ProgressFillExample]. */
@Composable
internal fun ProgressFillDemo() {
    var progress by remember { mutableFloatStateOf(0f) }
    var swatch by remember { mutableStateOf(Swatch.Cobalt) }
    var wave by remember { mutableFloatStateOf(4f) }
    val scope = rememberCoroutineScope()
    ExampleScaffold(
        autoplay = Autoplay(1.seconds) { sweepThereAndBack { progress = it } },
        stage = { ProgressFillExample(progress, swatch.color, wave.dp) },
        code = {
            progressEffectCode(
                call(
                    "ProgressEffect.fill",
                    "color" to "${swatch.color.kt()}.copy(alpha = 0.6f)",
                    "wave" to wave.dp.kt(),
                ),
            )
        },
        controls = {
            PrimaryPill("Play", onClick = { scope.launch { sweep { progress = it } } })
            LabeledSlider("Progress", "${(progress * 100).roundToInt()} %", progress, { progress = it }, 0f..1f)
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Wave", "${wave.roundToInt()} dp", wave, { wave = it }, 0f..12f)
        },
    )
}
