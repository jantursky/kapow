package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.TearExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The rims the tear page offers; [code] is null for the default. */
private enum class Rim(
    val color: Color,
    val code: String?,
) {
    Light(Color.White.copy(alpha = 0.75f), null),
    Amber(KapowEffectColors.amber, "Color(0xFFFFB547)"),
    None(Color.Transparent, "Color.Transparent"),
}

/** The controls around [TearExample]. */
@Composable
internal fun TearDemo() {
    var visible by remember { mutableStateOf(false) }
    var angle by remember { mutableFloatStateOf(0f) }
    var tooth by remember { mutableFloatStateOf(12f) }
    var seconds by remember { mutableFloatStateOf(0.5f) }
    var roughness by remember { mutableFloatStateOf(0.4f) }
    var rim by remember { mutableStateOf(Rim.Light) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { TearExample(visible, angle, tooth.roundToInt().dp, duration, roughness, rim.color) },
        code = {
            val args =
                transitionArgs(
                    "angle" to angle.kt(),
                    "toothSize" to tooth.roundToInt().dp.kt(),
                    "duration" to duration.kt(),
                    ("roughness" to roughness.kt()).takeIf { roughness != 0.4f },
                    rim.code?.let { "rimColor" to it },
                )
            transitionCode(call("KapowEnter.tear", *args), call("KapowExit.tear", *args))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            LabeledSlider("Angle", "${angle.roundToInt()}°", angle, { angle = it }, 0f..360f)
            LabeledSlider("Tooth size", "${tooth.roundToInt()} dp", tooth, { tooth = it }, 4f..32f)
            LabeledSlider("Roughness", roughness.oneDecimal(), roughness, { roughness = it }, 0f..1f)
            ChoiceRow("Rim", Rim.entries, rim, { rim = it }, { it.name })
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
