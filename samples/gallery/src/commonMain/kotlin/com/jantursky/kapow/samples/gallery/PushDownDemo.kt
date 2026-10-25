package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.PushDownExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [PushDownExample]. */
@Composable
internal fun PushDownDemo() {
    var pressed by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(0.95f) }
    var damping by remember { mutableFloatStateOf(0.6f) }
    var dim by remember { mutableFloatStateOf(0f) }
    var drop by remember { mutableFloatStateOf(0f) }
    ExampleScaffold(
        autoplay = Autoplay(1.5.seconds) { pressed = !pressed },
        stage = { PushDownExample(pressed, scale, damping, dim, drop.dp) },
        code = {
            val args =
                listOfNotNull(
                    "scale" to scale.kt(),
                    ("damping" to damping.kt()).takeIf { damping != 0.6f },
                    ("dim" to dim.kt()).takeIf { dim != 0f },
                    ("translation" to drop.dp.kt()).takeIf { drop != 0f },
                )
            conditionalEffectCode(call("ConditionalEffect.pushDown", *args.toTypedArray()), "pressed")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { pressed = !pressed })
            LabeledSlider("Scale", "${(scale * 100).roundToInt()} %", scale, { scale = it }, 0.5f..1f)
            LabeledSlider("Bounce", damping.oneDecimal(), damping, { damping = it }, 0.1f..1f)
            LabeledSlider("Dim", "${(dim * 100).roundToInt()} %", dim, { dim = it }, 0f..0.6f)
            LabeledSlider("Drop", "${drop.roundToInt()} dp", drop, { drop = it }, 0f..16f)
        },
    )
}
