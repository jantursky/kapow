package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    ExampleScaffold(
        autoplay = Autoplay(2.seconds) { likes++ },
        stage = { SprayExample(likes, shape, particles.roundToInt()) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.spray",
                    "particles" to call("ParticleContent.shape", "" to "ParticleShape.${shape.name}"),
                    "count" to particles.roundToInt().toString(),
                ),
                value = "likes",
            )
        },
        controls = {
            PrimaryPill("Like", onClick = { likes++ })
            ChoiceRow("Particle", ParticleShape.entries, shape, { shape = it }, { it.name })
            LabeledSlider("Count", particles.roundToInt().toString(), particles, { particles = it }, 1f..40f)
        },
    )
}
