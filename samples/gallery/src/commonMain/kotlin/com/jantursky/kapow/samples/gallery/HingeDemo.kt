package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.HingeExample
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [HingeExample]. */
@Composable
internal fun HingeDemo() {
    var angle by remember { mutableFloatStateOf(90f) }
    val scope = rememberCoroutineScope()
    ExampleScaffold(
        autoplay = Autoplay(1.seconds) { sweepThereAndBack { angle = it * 180f } },
        stage = { HingeExample(angle) },
        controls = {
            PrimaryPill("Unfold", onClick = { scope.launch { sweep { angle = it * 180f } } })
            LabeledSlider("Hinge angle", "${angle.roundToInt()}°", angle, { angle = it }, 0f..180f)
        },
    )
}
