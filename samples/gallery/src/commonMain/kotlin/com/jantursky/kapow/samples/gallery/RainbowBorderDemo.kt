package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.KapowToggle
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.RainbowBorderExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [RainbowBorderExample]. */
@Composable
internal fun RainbowBorderDemo() {
    var featured by remember { mutableStateOf(true) }
    var width by remember { mutableFloatStateOf(3f) }
    var turn by remember { mutableFloatStateOf(3f) }
    var clockwise by remember { mutableStateOf(true) }
    var glow by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (featured) 5.seconds else 1.5.seconds }, startsOn = true) { featured = !featured },
        stage = {
            RainbowBorderExample(featured, width.dp, (turn * 1000).roundToInt().milliseconds, clockwise, glow.dp, alpha)
        },
        code = {
            val args =
                listOfNotNull(
                    "width" to width.dp.kt(),
                    "shape" to "RoundedCornerShape(20.dp)",
                    "duration" to (turn * 1000).roundToInt().milliseconds.kt(),
                    ("clockwise" to clockwise).takeIf { !clockwise },
                    ("glow" to glow.dp.kt()).takeIf { glow != 0f },
                    ("alpha" to alpha.kt()).takeIf { alpha != 1f },
                )
            conditionalEffectCode(call("ConditionalEffect.rainbowBorder", *args.toTypedArray()), "featured")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { featured = !featured })
            LabeledSlider("Width", "${width.oneDecimal()} dp", width, { width = it }, 1f..8f)
            LabeledSlider("Turn", "${turn.oneDecimal()} s", turn, { turn = it }, 0.5f..8f)
            LabeledSlider("Glow", "${glow.roundToInt()} dp", glow, { glow = it }, 0f..16f)
            LabeledSlider("Alpha", "${(alpha * 100).roundToInt()} %", alpha, { alpha = it }, 0.1f..1f)
            KapowToggle(clockwise, { clockwise = it }, "Clockwise")
        },
    )
}
