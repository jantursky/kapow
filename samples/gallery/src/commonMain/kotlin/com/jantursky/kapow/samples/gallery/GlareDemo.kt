package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.GlareExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [GlareExample]. */
@Composable
internal fun GlareDemo() {
    var visible by remember { mutableStateOf(false) }
    var angle by remember { mutableFloatStateOf(45f) }
    var swatch by remember { mutableStateOf(Swatch.Amber) }
    var seconds by remember { mutableFloatStateOf(0.5f) }
    var soft by remember { mutableFloatStateOf(0f) }
    var streak by remember { mutableFloatStateOf(0f) }
    var intensity by remember { mutableFloatStateOf(1f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = {
            GlareExample(
                visible,
                angle,
                swatch.color,
                duration,
                soft.roundToInt().dp,
                if (streak >= 1f) streak.roundToInt().dp else Dp.Unspecified,
                intensity,
            )
        },
        code = {
            val args =
                transitionArgs(
                    "angle" to angle.kt(),
                    "color" to swatch.color.kt(),
                    "duration" to duration.kt(),
                    ("softEdge" to soft.roundToInt().dp.kt()).takeIf { soft >= 1f },
                    ("width" to streak.roundToInt().dp.kt()).takeIf { streak >= 1f },
                    ("intensity" to intensity.kt()).takeIf { intensity < 1f },
                )
            transitionCode(call("KapowEnter.glare", *args), call("KapowExit.glare", *args))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            LabeledSlider("Angle", "${angle.roundToInt()}°", angle, { angle = it }, 0f..360f)
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Soft edge", "${soft.roundToInt()} dp", soft, { soft = it }, 0f..60f)
            LabeledSlider(
                "Streak width",
                if (streak >= 1f) "${streak.roundToInt()} dp" else "Auto",
                streak,
                { streak = it },
                0f..120f,
            )
            LabeledSlider("Intensity", intensity.oneDecimal(), intensity, { intensity = it }, 0f..1f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
