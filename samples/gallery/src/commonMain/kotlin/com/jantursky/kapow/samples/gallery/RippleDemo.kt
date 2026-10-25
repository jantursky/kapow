package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.RippleExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Where the ripple on the to-do row starts. */
private enum class RippleFrom(
    val origin: TransformOrigin,
) {
    Checkbox(TransformOrigin(0.11f, 0.5f)),
    Center(TransformOrigin.Center),
    End(TransformOrigin(1f, 0.5f)),
}

/** The controls around [RippleExample]. */
@Composable
internal fun RippleDemo() {
    var done by remember { mutableStateOf(false) }
    var swatch by remember { mutableStateOf(Swatch.Cobalt) }
    var from by remember { mutableStateOf(RippleFrom.Checkbox) }
    var seconds by remember { mutableFloatStateOf(0.6f) }
    var rings by remember { mutableFloatStateOf(1f) }
    var ringWidth by remember { mutableFloatStateOf(0f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.seconds) { done = !done },
        stage = { RippleExample(done, swatch.color, from.origin, duration, rings.roundToInt(), ringWidth.ring()) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.ripple",
                    "color" to swatch.color.copy(alpha = 0.35f).kt(),
                    "shape" to "RoundedCornerShape(16.dp)",
                    "origin" to from.kt(),
                    "duration" to duration.kt(),
                    "rings" to rings.roundToInt().toString(),
                    "ringWidth" to (if (ringWidth < 1f) "Dp.Unspecified" else ringWidth.ring().kt()),
                ),
                value = "done",
            )
        },
        controls = {
            PrimaryPill(if (done) "Undo" else "Done", onClick = { done = !done })
            ChoiceRow("From", RippleFrom.entries, from, { from = it }, { it.name })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..2f)
            LabeledSlider("Rings", rings.roundToInt().toString(), rings, { rings = it }, 1f..4f)
            LabeledSlider(
                "Ring width",
                if (ringWidth <
                    1f
                ) {
                    "filled"
                } else {
                    "${ringWidth.roundToInt()} dp"
                },
                ringWidth,
                { ringWidth = it },
                0f..12f,
            )
        },
    )
}

/** The origin as the source a user would write. */
private fun RippleFrom.kt(): String =
    when (this) {
        RippleFrom.Checkbox -> "TransformOrigin(0.11f, 0.5f)"
        RippleFrom.Center -> "TransformOrigin.Center"
        RippleFrom.End -> "TransformOrigin(1f, 0.5f)"
    }

/** The slider's width as a ring width: below 1 the ripple is a filled disc. */
private fun Float.ring(): Dp = if (this < 1f) Dp.Unspecified else dp
