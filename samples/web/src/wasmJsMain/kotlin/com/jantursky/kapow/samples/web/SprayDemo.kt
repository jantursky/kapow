package com.jantursky.kapow.samples.web

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
import com.jantursky.kapow.samples.web.examples.SprayExample
import kotlin.math.roundToInt

/** The controls around [SprayExample]. */
@Composable
internal fun SprayDemo() {
    var likes by remember { mutableIntStateOf(0) }
    var shape by remember { mutableStateOf(ParticleShape.Heart) }
    var particles by remember { mutableFloatStateOf(11f) }
    ExampleScaffold(
        stage = { SprayExample(likes, shape, particles.roundToInt()) },
        controls = {
            PrimaryPill("Like", onClick = { likes++ })
            ChoiceRow("Particle", ParticleShape.entries, shape, { shape = it }, { it.name })
            LabeledSlider("Count", particles.roundToInt().toString(), particles, { particles = it }, 1f..40f)
        },
    )
}
