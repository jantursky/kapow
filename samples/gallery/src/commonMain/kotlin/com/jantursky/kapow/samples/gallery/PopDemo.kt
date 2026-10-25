package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.ParticlePhysics
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.PopExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [PopExample]. */
@Composable
internal fun PopDemo() {
    var visible by remember { mutableStateOf(false) }
    var swatch by remember { mutableStateOf(Swatch.Amber) }
    var seconds by remember { mutableFloatStateOf(1.2f) }
    var overshoot by remember { mutableFloatStateOf(1f) }
    var rings by remember { mutableFloatStateOf(1f) }
    var particles by remember { mutableFloatStateOf(12f) }
    var gravity by remember { mutableFloatStateOf(0f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    val gravityDp = (gravity / 10).roundToInt() * 10
    val physics = if (gravityDp > 0) ParticlePhysics.of(gravity = gravityDp.toFloat()) else ParticlePhysics.Default
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = {
            PopExample(visible, swatch.color, duration, overshoot, rings.roundToInt(), particles.roundToInt(), physics)
        },
        code = {
            transitionCode(
                enter =
                    call(
                        "KapowEnter.pop",
                        *transitionArgs(
                            "color" to swatch.color.kt(),
                            "duration" to duration.kt(),
                            ("overshoot" to overshoot.kt()).takeIf { overshoot != 1f },
                            ("ringCount" to rings.roundToInt()).takeIf { rings.roundToInt() != 1 },
                            ("particleCount" to particles.roundToInt()).takeIf { particles.roundToInt() != 12 },
                            ("physics" to "ParticlePhysics.of(gravity = ${gravityDp.toFloat().kt()})")
                                .takeIf { gravityDp > 0 },
                        ),
                    ),
            )
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Overshoot", overshoot.oneDecimal(), overshoot, { overshoot = it }, 0f..3f)
            LabeledSlider("Rings", "${rings.roundToInt()}", rings, { rings = it }, 0f..5f)
            LabeledSlider("Particles", "${particles.roundToInt()}", particles, { particles = it }, 0f..100f)
            LabeledSlider("Gravity", "$gravityDp dp/s²", gravity, { gravity = it }, 0f..1000f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
