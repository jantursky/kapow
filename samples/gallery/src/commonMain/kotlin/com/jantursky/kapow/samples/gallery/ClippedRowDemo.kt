package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ClippedRowExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [ClippedRowExample]. */
@Composable
internal fun ClippedRowDemo() {
    var likes by remember { mutableIntStateOf(0) }
    var particles by remember { mutableFloatStateOf(11f) }
    ExampleScaffold(
        autoplay = Autoplay(2.seconds) { likes++ },
        stage = { ClippedRowExample(likes, particles.roundToInt()) },
        code = {
            val spray =
                call(
                    "ChangeEffect.spray",
                    "particles" to "ParticleContent.shape(ParticleShape.Heart)",
                    "count" to particles.roundToInt(),
                )
            "val spray = $spray\n\nKapowParticleLayer {\n    Box(Modifier.changeEffect(likes, spray)) {\n" +
                "        // your content\n    }\n}"
        },
        controls = {
            PrimaryPill("Like", onClick = { likes++ })
            LabeledSlider("Count", particles.roundToInt().toString(), particles, { particles = it }, 1f..40f)
        },
    )
}
