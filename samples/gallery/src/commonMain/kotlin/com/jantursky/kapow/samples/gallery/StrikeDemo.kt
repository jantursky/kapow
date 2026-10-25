package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.StrikeDirection
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.StrikeExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [StrikeExample]. */
@Composable
internal fun StrikeDemo() {
    var done by remember { mutableStateOf(false) }
    var swatch by remember { mutableStateOf(Swatch.Coral) }
    var seconds by remember { mutableFloatStateOf(0.6f) }
    var direction by remember { mutableStateOf(StrikeDirection.Reading) }
    var lines by remember { mutableFloatStateOf(1f) }
    var wave by remember { mutableFloatStateOf(0f) }
    val hold = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        // Done stays long enough for the line to draw, hold and fade; not done is a short pause.
        autoplay = Autoplay({ if (done) hold + 1.2.seconds else 0.8.seconds }) { done = !done },
        stage = { StrikeExample(done, swatch.color, hold, direction, lines.roundToInt(), wave.dp) },
        code = {
            call(
                "Modifier.changeEffect",
                "" to "done",
                "" to
                    call(
                        "ChangeEffect.strike",
                        "color" to swatch.color.kt(),
                        "hold" to hold.kt(),
                        "direction" to "StrikeDirection.${direction.name}",
                        "lines" to lines.roundToInt().toString(),
                        "waveAmplitude" to wave.dp.kt(),
                    ),
                "isEnabled" to "done",
            ).render()
        },
        controls = {
            PrimaryPill("Toggle", onClick = { done = !done })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Hold", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0f..2f)
            ChoiceRow("Direction", StrikeDirection.entries, direction, { direction = it }, { it.name })
            LabeledSlider("Lines", lines.roundToInt().toString(), lines, { lines = it }, 1f..3f)
            LabeledSlider("Wave", "${wave.roundToInt()} dp", wave, { wave = it }, 0f..8f)
        },
    )
}
