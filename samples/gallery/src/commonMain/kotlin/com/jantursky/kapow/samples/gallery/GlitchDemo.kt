package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.GlitchExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [GlitchExample]. */
@Composable
internal fun GlitchDemo() {
    var errors by remember { mutableIntStateOf(0) }
    var millis by remember { mutableFloatStateOf(300f) }
    var offset by remember { mutableFloatStateOf(6f) }
    var slice by remember { mutableFloatStateOf(0.3f) }
    var jump by remember { mutableFloatStateOf(0.5f) }
    val duration = millis.roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.4.seconds) { errors++ },
        stage = { GlitchExample(errors, duration, offset.dp, 0.08f..slice, jump) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.glitch",
                    "duration" to duration.kt(),
                    "offset" to offset.dp.kt(),
                    "sliceHeight" to "0.08f..${slice.twoDecimals()}f",
                    "jumpChance" to "${jump.oneDecimal()}f",
                ),
                value = "errors",
            )
        },
        controls = {
            PrimaryPill("Fail", onClick = { errors++ })
            LabeledSlider("Duration", "${millis.roundToInt()} ms", millis, { millis = it }, 100f..1000f)
            LabeledSlider("Offset", "${offset.roundToInt()} dp", offset, { offset = it }, 0f..16f)
            LabeledSlider("Slice height", "${(slice * 100).roundToInt()} %", slice, { slice = it }, 0.1f..0.6f)
            LabeledSlider("Jumps", "${(jump * 100).roundToInt()} %", jump, { jump = it }, 0f..1f)
        },
    )
}

private fun Float.twoDecimals(): String = "${(this * 100).roundToInt() / 100f}"
