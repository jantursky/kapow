package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.StampExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [StampExample]. */
@Composable
internal fun StampDemo() {
    var approvals by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1.3f) }
    var tilt by remember { mutableFloatStateOf(0f) }
    var dust by remember { mutableFloatStateOf(5f) }
    ExampleScaffold(
        autoplay = Autoplay(1.8.seconds) { approvals++ },
        stage = { StampExample(approvals, scale, tilt, dust.roundToInt()) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.stamp",
                    "scale" to scale.kt(),
                    "tilt" to "${tilt.roundToInt()}f",
                    "dustCount" to dust.roundToInt().toString(),
                ),
                value = "approvals",
            )
        },
        controls = {
            PrimaryPill("Approve", onClick = { approvals++ })
            LabeledSlider("Scale", "${(scale * 100).roundToInt()} %", scale, { scale = it }, 1f..2f)
            LabeledSlider("Tilt", "${tilt.roundToInt()}°", tilt, { tilt = it }, -30f..30f)
            LabeledSlider("Dust", dust.roundToInt().toString(), dust, { dust = it }, 0f..12f)
        },
    )
}
