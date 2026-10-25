package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.DisintegrateExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Where the dust gets its color: the content itself, or one color. */
private enum class Dust(
    val color: Color,
) {
    Content(Color.Unspecified),
    Gray(Color(0xFF8A8A8A)),
    Amber(KapowEffectColors.amber),
}

/** The controls around [DisintegrateExample]. */
@Composable
internal fun DisintegrateDemo() {
    var visible by remember { mutableStateOf(true) }
    var dust by remember { mutableStateOf(Dust.Content) }
    var seconds by remember { mutableFloatStateOf(1.5f) }
    var grain by remember { mutableFloatStateOf(3f) }
    var wind by remember { mutableFloatStateOf(0f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { DisintegrateExample(visible, dust.color, duration, grain.roundToInt().dp, wind) },
        code = {
            val exit =
                call(
                    "KapowExit.disintegrate",
                    *transitionArgs(
                        ("color" to dust.color.kt()).takeIf { dust.color.isSpecified },
                        "duration" to duration.kt(),
                        ("particleSize" to grain.roundToInt().dp.kt()).takeIf { grain.roundToInt() != 3 },
                        ("wind" to wind.kt()).takeIf { wind != 0f },
                    ),
                )
            transitionCode(exit = exit)
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Dust", Dust.entries, dust, { dust = it }, { it.name })
            LabeledSlider("Particle size", "${grain.roundToInt()} dp", grain, { grain = it }, 1f..16f)
            LabeledSlider("Wind", wind.oneDecimal(), wind, { wind = it }, -1f..1f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.5f..4f)
        },
    )
}
