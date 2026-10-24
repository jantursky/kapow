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
import com.jantursky.kapow.samples.gallery.examples.ProgressTraceExample
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [ProgressTraceExample]. */
@Composable
internal fun ProgressTraceDemo() {
    var progress by remember { mutableFloatStateOf(0f) }
    var swatch by remember { mutableStateOf(Swatch.Violet) }
    var width by remember { mutableFloatStateOf(3f) }
    val scope = rememberCoroutineScope()
    ExampleScaffold(
        autoplay = Autoplay(1.seconds) { sweepThereAndBack { progress = it } },
        stage = { ProgressTraceExample(progress, swatch.color, width.dp) },
        code = {
            progressEffectCode(
                call(
                    "ProgressEffect.trace",
                    "shape" to "RoundedCornerShape(24.dp)",
                    "color" to swatch.color.kt(),
                    "width" to width.dp.kt(),
                ),
            )
        },
        controls = {
            PrimaryPill("Play", onClick = { scope.launch { sweep { progress = it } } })
            LabeledSlider("Progress", "${(progress * 100).roundToInt()} %", progress, { progress = it }, 0f..1f)
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Width", "${width.roundToInt()} dp", width, { width = it }, 1f..8f)
        },
    )
}
