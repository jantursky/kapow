package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ProgressShineExample
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [ProgressShineExample]. */
@Composable
internal fun ProgressShineDemo() {
    var progress by remember { mutableFloatStateOf(0f) }
    var angle by remember { mutableFloatStateOf(45f) }
    var bandWidth by remember { mutableFloatStateOf(0.6f) }
    var softness by remember { mutableFloatStateOf(1f) }
    var bands by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    ExampleScaffold(
        autoplay = Autoplay(1.seconds) { sweepThereAndBack { progress = it } },
        stage = { ProgressShineExample(progress, angle, Color.White, bandWidth, softness, bands.roundToInt()) },
        code = {
            val args =
                listOfNotNull(
                    "angle" to angle.kt(),
                    "color" to "Color.White",
                    ("bandWidth" to bandWidth.kt()).takeIf { bandWidth != 0.6f },
                    ("softness" to softness.kt()).takeIf { softness != 1f },
                    ("bands" to bands.roundToInt()).takeIf { bands.roundToInt() != 1 },
                )
            progressEffectCode(call("ProgressEffect.shine", *args.toTypedArray()))
        },
        controls = {
            PrimaryPill("Play", onClick = { scope.launch { sweep { progress = it } } })
            LabeledSlider("Progress", "${(progress * 100).roundToInt()} %", progress, { progress = it }, 0f..1f)
            LabeledSlider("Angle", "${angle.roundToInt()}°", angle, { angle = it }, 0f..360f)
            LabeledSlider(
                "Band width",
                "${(bandWidth * 100).roundToInt()} %",
                bandWidth,
                { bandWidth = it },
                0.1f..1.2f,
            )
            LabeledSlider("Softness", softness.oneDecimal(), softness, { softness = it }, 0.3f..3f)
            LabeledSlider("Bands", "${bands.roundToInt()}", bands, { bands = it }, 1f..5f)
        },
    )
}
