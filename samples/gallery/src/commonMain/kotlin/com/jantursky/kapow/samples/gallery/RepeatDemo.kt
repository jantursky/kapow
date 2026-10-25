package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.RepeatExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [RepeatExample]. */
@Composable
internal fun RepeatDemo() {
    var unread by remember { mutableStateOf(true) }
    var seconds by remember { mutableFloatStateOf(1f) }
    var maxCount by remember { mutableFloatStateOf(0f) }
    var jitter by remember { mutableFloatStateOf(0f) }
    var delay by remember { mutableFloatStateOf(0f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay =
            Autoplay({ if (unread) maxOf(4.seconds, duration * 3) else 1.5.seconds }, startsOn = true) {
                unread = !unread
            },
        stage = {
            RepeatExample(
                unread,
                duration,
                (delay * 1000).roundToInt().milliseconds,
                maxCount.roundToInt().takeIf {
                    it >
                        0
                },
                jitter,
            )
        },
        code = {
            val args =
                listOfNotNull(
                    "" to
                        call(
                            "ChangeEffect.pulse",
                            "shape" to "CircleShape",
                            "color" to Swatch.Coral.color.kt(),
                            "style" to "PulseStyle.Fill",
                        ),
                    "every" to duration.kt(),
                    ("initialDelay" to (delay * 1000).roundToInt().milliseconds.kt()).takeIf { delay != 0f },
                    ("maxCount" to maxCount.roundToInt()).takeIf { maxCount.roundToInt() > 0 },
                    ("jitter" to jitter.kt()).takeIf { jitter != 0f },
                )
            conditionalEffectCode(call("ConditionalEffect.repeat", *args.toTypedArray()), "unread")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { unread = !unread })
            LabeledSlider("Every", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.1f..3f)
            LabeledSlider("Start after", "${delay.oneDecimal()} s", delay, { delay = it }, 0f..2f)
            LabeledSlider(
                "Max plays",
                if (maxCount.roundToInt() ==
                    0
                ) {
                    "no limit"
                } else {
                    "${maxCount.roundToInt()}"
                },
                maxCount,
                { maxCount = it },
                0f..10f,
            )
            LabeledSlider("Jitter", "${(jitter * 100).roundToInt()} %", jitter, { jitter = it }, 0f..1f)
        },
    )
}
