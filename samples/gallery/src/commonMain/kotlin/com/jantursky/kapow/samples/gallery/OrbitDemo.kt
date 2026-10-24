package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    ExampleScaffold(
        autoplay = Autoplay({ if (online) 5.seconds else 1.5.seconds }, startsOn = true) { online = !online },
        stage = { OrbitExample(online, count.roundToInt(), (turn * 1000).roundToInt().milliseconds) },
        code = {
            conditionalEffectCode(
                call(
                    "ConditionalEffect.orbit",
                    "count" to count.roundToInt(),
                    "duration" to (turn * 1000).roundToInt().milliseconds.kt(),
                ),
                "online",
            )
        },
        controls = {
            PrimaryPill("Toggle", onClick = { online = !online })
            LabeledSlider("Count", "${count.roundToInt()}", count, { count = it }, 1f..8f)
            LabeledSlider("Turn", "${turn.oneDecimal()} s", turn, { turn = it }, 0.5f..6f)
        },
    )
}
