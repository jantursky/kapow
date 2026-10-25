package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.KapowToggle
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.OrbitExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [OrbitExample]. */
@Composable
internal fun OrbitDemo() {
    var online by remember { mutableStateOf(true) }
    var count by remember { mutableFloatStateOf(3f) }
    var turn by remember { mutableFloatStateOf(2.5f) }
    var clockwise by remember { mutableStateOf(true) }
    var radius by remember { mutableFloatStateOf(1.6f) }
    var trail by remember { mutableFloatStateOf(4f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (online) 5.seconds else 1.5.seconds }, startsOn = true) { online = !online },
        stage = {
            OrbitExample(
                online,
                count.roundToInt(),
                (turn * 1000).roundToInt().milliseconds,
                clockwise,
                radius,
                trail.roundToInt(),
            )
        },
        code = {
            val args =
                listOfNotNull(
                    "count" to count.roundToInt(),
                    "duration" to (turn * 1000).roundToInt().milliseconds.kt(),
                    ("clockwise" to clockwise).takeIf { !clockwise },
                    ("radius" to radius.kt()).takeIf { radius != 1.6f },
                    ("trail" to trail.roundToInt()).takeIf { trail.roundToInt() != 4 },
                )
            conditionalEffectCode(call("ConditionalEffect.orbit", *args.toTypedArray()), "online")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { online = !online })
            LabeledSlider("Count", "${count.roundToInt()}", count, { count = it }, 1f..8f)
            LabeledSlider("Turn", "${turn.oneDecimal()} s", turn, { turn = it }, 0.5f..6f)
            LabeledSlider("Radius", "${radius.oneDecimal()}x", radius, { radius = it }, 1f..2.5f)
            LabeledSlider("Trail", "${trail.roundToInt()}", trail, { trail = it }, 0f..10f)
            KapowToggle(clockwise, { clockwise = it }, "Clockwise")
        },
    )
}
