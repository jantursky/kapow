package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.BlindsOrientation
import com.jantursky.kapow.samples.design.KapowToggle
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.BlindsExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [BlindsExample]. */
@Composable
internal fun BlindsDemo() {
    var visible by remember { mutableStateOf(false) }
    var width by remember { mutableFloatStateOf(10f) }
    var orientation by remember { mutableStateOf(BlindsOrientation.Horizontal) }
    var staggered by remember { mutableStateOf(false) }
    var seconds by remember { mutableFloatStateOf(0.5f) }
    var count by remember { mutableFloatStateOf(0f) }
    var amount by remember { mutableFloatStateOf(1f) }
    var reverse by remember { mutableStateOf(false) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    val slats = count.roundToInt().takeIf { it > 0 }
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = {
            BlindsExample(
                visible,
                width.dp,
                orientation,
                staggered,
                duration,
                slats,
                amount,
                reverse,
            )
        },
        code = {
            val args =
                transitionArgs(
                    ("slatWidth" to width.dp.kt()).takeIf { slats == null },
                    "orientation" to orientation.kt(),
                    "staggered" to staggered,
                    "duration" to duration.kt(),
                    slats?.let { "slatCount" to it },
                    ("staggerAmount" to amount.kt()).takeIf { staggered && amount != 1f },
                    ("reverse" to true).takeIf { staggered && reverse },
                )
            transitionCode(call("KapowEnter.blinds", *args), call("KapowExit.blinds", *args))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Direction", BlindsOrientation.entries, orientation, { orientation = it }, { it.name })
            LabeledSlider("Slat width", "${width.roundToInt()} dp", width, { width = it }, 5f..40f)
            LabeledSlider("Slat count", slats?.toString() ?: "By width", count, { count = it }, 0f..24f)
            KapowToggle(staggered, { staggered = it }, "Staggered")
            LabeledSlider("Stagger amount", amount.oneDecimal(), amount, { amount = it }, 0f..3f)
            KapowToggle(reverse, { reverse = it }, "Reverse order")
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
