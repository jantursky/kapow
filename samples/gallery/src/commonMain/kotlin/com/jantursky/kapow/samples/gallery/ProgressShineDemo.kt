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
    val scope = rememberCoroutineScope()
    ExampleScaffold(
        autoplay = Autoplay(1.seconds) { sweepThereAndBack { progress = it } },
        stage = { ProgressShineExample(progress, angle, Color.White) },
        controls = {
            PrimaryPill("Play", onClick = { scope.launch { sweep { progress = it } } })
            LabeledSlider("Progress", "${(progress * 100).roundToInt()} %", progress, { progress = it }, 0f..1f)
            LabeledSlider("Angle", "${angle.roundToInt()}°", angle, { angle = it }, 0f..360f)
        },
    )
}
