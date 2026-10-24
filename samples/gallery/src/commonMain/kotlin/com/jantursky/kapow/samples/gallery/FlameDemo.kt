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
import com.jantursky.kapow.samples.gallery.examples.FlameExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [FlameExample]. */
@Composable
internal fun FlameDemo() {
    var onFire by remember { mutableStateOf(true) }
    var height by remember { mutableFloatStateOf(40f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (onFire) 4.seconds else 1.5.seconds }, startsOn = true) { onFire = !onFire },
        stage = { FlameExample(onFire, if (onFire) 12 else 0, height.dp) },
        code = {
            conditionalEffectCode(
                call(
                    "ConditionalEffect.flame",
                    "height" to height.dp.kt(),
                ),
                "onFire",
            )
        },
        controls = {
            PrimaryPill("Toggle", onClick = { onFire = !onFire })
            LabeledSlider("Height", "${height.roundToInt()} dp", height, { height = it }, 16f..80f)
        },
    )
}
