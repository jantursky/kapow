package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    ExampleScaffold(
        autoplay = Autoplay(1.5.seconds) { pressed = !pressed },
        stage = { PushDownExample(pressed, scale) },
        code = {
            conditionalEffectCode(
                call(
                    "ConditionalEffect.pushDown",
                    "scale" to scale.kt(),
                ),
                "pressed",
            )
        },
        controls = {
            PrimaryPill("Toggle", onClick = { pressed = !pressed })
            LabeledSlider("Scale", "${(scale * 100).roundToInt()} %", scale, { scale = it }, 0.5f..1f)
        },
    )
}
