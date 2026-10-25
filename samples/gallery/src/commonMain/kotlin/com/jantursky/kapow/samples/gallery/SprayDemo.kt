package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.ColorPick
import com.jantursky.kapow.ParticlePhysics
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.SprayExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [SprayExample]. */
@Composable
internal fun SprayDemo() {
    var likes by remember { mutableIntStateOf(0) }
    var shape by remember { mutableStateOf(ParticleShape.Heart) }
    var particles by remember { mutableFloatStateOf(11f) }
    var spread by remember { mutableFloatStateOf(100f) }
    var gravity by remember { mutableFloatStateOf(600f) }
    var pick by remember { mutableStateOf(ColorPick.Sequential) }
    ExampleScaffold(
        autoplay = Autoplay(2.seconds) { likes++ },
        stage = { SprayExample(likes, shape, particles.roundToInt(), physics(spread, gravity, pick)) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.spray",
                    "particles" to call("ParticleContent.shape", "" to "ParticleShape.${shape.name}"),
                    "count" to particles.roundToInt().toString(),
                    "physics" to
                        call(
                            "ParticlePhysics.of",
                            "spread" to "${spread.roundToInt()}f",
                            "gravity" to "${gravity.roundToInt()}f",
                            "colorPick" to "ColorPick.${pick.name}",
                        ),
                ),
                value = "likes",
            )
        },
        controls = {
            PrimaryPill("Like", onClick = { likes++ })
            ChoiceRow("Particle", ParticleShape.entries, shape, { shape = it }, { it.name })
            LabeledSlider("Count", particles.roundToInt().toString(), particles, { particles = it }, 1f..40f)
            LabeledSlider("Spread", "${spread.roundToInt()}°", spread, { spread = it }, 0f..360f)
            LabeledSlider("Gravity", gravity.roundToInt().toString(), gravity, { gravity = it }, -600f..1200f)
            ChoiceRow("Colors", ColorPick.entries, pick, { pick = it }, { it.name })
        },
    )
}

private fun physics(
    spread: Float,
    gravity: Float,
    pick: ColorPick,
): ParticlePhysics = ParticlePhysics.of(spread = spread, gravity = gravity, colorPick = pick)
