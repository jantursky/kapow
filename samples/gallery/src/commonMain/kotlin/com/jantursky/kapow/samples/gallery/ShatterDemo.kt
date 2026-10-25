package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ShatterExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The places the content can be hit; [code] is null for the default middle. */
private enum class Impact(
    val origin: TransformOrigin,
    val code: String?,
) {
    Center(TransformOrigin.Center, null),
    TopLeft(TransformOrigin(0f, 0f), "TransformOrigin(0f, 0f)"),
    TopRight(TransformOrigin(1f, 0f), "TransformOrigin(1f, 0f)"),
    BottomLeft(TransformOrigin(0f, 1f), "TransformOrigin(0f, 1f)"),
}

/** The controls around [ShatterExample]. */
@Composable
internal fun ShatterDemo() {
    var visible by remember { mutableStateOf(true) }
    var shards by remember { mutableFloatStateOf(24f) }
    var seconds by remember { mutableFloatStateOf(0.9f) }
    var gravity by remember { mutableFloatStateOf(1f) }
    var spin by remember { mutableFloatStateOf(1f) }
    var impact by remember { mutableStateOf(Impact.Center) }
    var seed by remember { mutableFloatStateOf(61f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = {
            ShatterExample(visible, shards.roundToInt(), duration, gravity, spin, impact.origin, seed.roundToInt())
        },
        code = {
            val exit =
                call(
                    "KapowExit.shatter",
                    *transitionArgs(
                        "shards" to shards.roundToInt(),
                        "duration" to duration.kt(),
                        ("gravity" to gravity.kt()).takeIf { gravity != 1f },
                        ("spin" to spin.kt()).takeIf { spin != 1f },
                        impact.code?.let { "impact" to it },
                        ("seed" to seed.roundToInt()).takeIf { seed.roundToInt() != 61 },
                    ),
                )
            transitionCode(exit = exit)
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            LabeledSlider("Shards", "${shards.roundToInt()}", shards, { shards = it }, 6f..80f)
            LabeledSlider("Gravity", gravity.oneDecimal(), gravity, { gravity = it }, 0f..3f)
            LabeledSlider("Spin", spin.oneDecimal(), spin, { spin = it }, 0f..3f)
            ChoiceRow("Impact", Impact.entries, impact, { impact = it }, { it.name })
            LabeledSlider("Seed", "${seed.roundToInt()}", seed, { seed = it }, 1f..100f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.3f..3f)
        },
    )
}
