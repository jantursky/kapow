package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.EdgeSide
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.FlameExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [FlameExample]. */
@Composable
internal fun FlameDemo() {
    var onFire by remember { mutableStateOf(true) }
    var height by remember { mutableFloatStateOf(40f) }
    var intensity by remember { mutableFloatStateOf(1f) }
    var wind by remember { mutableFloatStateOf(0f) }
    var edge by remember { mutableStateOf(EdgeSide.Top) }
    ExampleScaffold(
        autoplay = Autoplay({ if (onFire) 4.seconds else 1.5.seconds }, startsOn = true) { onFire = !onFire },
        stage = { FlameExample(onFire, if (onFire) 12 else 0, height.dp, intensity, wind, edge) },
        code = {
            val args =
                listOfNotNull(
                    "height" to height.dp.kt(),
                    ("intensity" to intensity.kt()).takeIf { intensity != 1f },
                    ("wind" to wind.kt()).takeIf { wind != 0f },
                    ("edge" to "EdgeSide.${edge.name}").takeIf { edge != EdgeSide.Top },
                )
            conditionalEffectCode(call("ConditionalEffect.flame", *args.toTypedArray()), "onFire")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { onFire = !onFire })
            LabeledSlider("Height", "${height.roundToInt()} dp", height, { height = it }, 16f..80f)
            LabeledSlider("Intensity", "${intensity.oneDecimal()}x", intensity, { intensity = it }, 0.3f..3f)
            LabeledSlider("Wind", "${wind.roundToInt()} dp/s²", wind, { wind = it }, -150f..150f)
            ChoiceRow("Edge", EdgeSide.entries, edge, { edge = it }, { it.name })
        },
    )
}
