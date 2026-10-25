package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.AnvilExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The dust colors the anvil page offers; [code] is null for the default. */
private enum class AnvilDust(
    val color: Color,
    val code: String?,
) {
    Gray(Color(0xFFB0B0B0), null),
    Amber(KapowEffectColors.amber, "Color(0xFFFFB547)"),
    Coral(KapowEffectColors.coral, "Color(0xFFFF6B5A)"),
}

/** The controls around [AnvilExample]. */
@Composable
internal fun AnvilDemo() {
    var visible by remember { mutableStateOf(false) }
    var seconds by remember { mutableFloatStateOf(1.4f) }
    var drop by remember { mutableFloatStateOf(0f) }
    var shake by remember { mutableFloatStateOf(1f) }
    var dust by remember { mutableStateOf(AnvilDust.Gray) }
    var dustCount by remember { mutableFloatStateOf(6f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    val dropHeight = if (drop < 1f) Dp.Unspecified else drop.roundToInt().dp
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { AnvilExample(visible, duration, dropHeight, shake, dust.color, dustCount.roundToInt()) },
        code = {
            transitionCode(
                enter =
                    call(
                        "KapowEnter.anvil",
                        *transitionArgs(
                            "duration" to duration.kt(),
                            if (drop >= 1f) "dropHeight" to dropHeight.kt() else null,
                            ("shake" to shake.kt()).takeIf { shake != 1f },
                            dust.code?.let { "dustColor" to it },
                            ("dustCount" to dustCount.roundToInt()).takeIf { dustCount.roundToInt() != 6 },
                        ),
                    ),
            )
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            LabeledSlider(
                "Drop height",
                if (drop < 1f) "auto" else "${drop.roundToInt()} dp",
                drop,
                { drop = it },
                0f..400f,
            )
            LabeledSlider("Shake", shake.oneDecimal(), shake, { shake = it }, 0f..3f)
            ChoiceRow("AnvilDust", AnvilDust.entries, dust, { dust = it }, { it.name })
            LabeledSlider("AnvilDust count", "${dustCount.roundToInt()}", dustCount, { dustCount = it }, 0f..40f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
