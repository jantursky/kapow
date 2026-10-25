package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ConditionalSparklesExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [ConditionalSparklesExample]. */
@Composable
internal fun ConditionalSparklesDemo() {
    var isNew by remember { mutableStateOf(true) }
    var count by remember { mutableFloatStateOf(4f) }
    var size by remember { mutableFloatStateOf(18f) }
    var lifetime by remember { mutableFloatStateOf(1f) }
    var shape by remember { mutableStateOf(ParticleShape.Sparkle) }
    var seed by remember { mutableFloatStateOf(0f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (isNew) 4.seconds else 1.5.seconds }, startsOn = true) { isNew = !isNew },
        stage = { ConditionalSparklesExample(isNew, count.roundToInt(), size.dp, lifetime, shape, seed.roundToInt()) },
        code = {
            val args =
                listOfNotNull(
                    "size" to size.dp.kt(),
                    "count" to count.roundToInt(),
                    ("lifetime" to "${(lifetime * 0.8f).kt()}..${(lifetime * 1.3f).kt()}").takeIf { lifetime != 1f },
                    ("shape" to "ParticleShape.${shape.name}").takeIf { shape != ParticleShape.Sparkle },
                    ("seed" to seed.roundToInt()).takeIf { seed.roundToInt() != 0 },
                )
            conditionalEffectCode(call("ConditionalEffect.sparkles", *args.toTypedArray()), "isNew")
        },
        controls = {
            PrimaryPill("Toggle", onClick = { isNew = !isNew })
            LabeledSlider("Count", "${count.roundToInt()}", count, { count = it }, 1f..12f)
            LabeledSlider("Size", "${size.roundToInt()} dp", size, { size = it }, 8f..32f)
            LabeledSlider("Lifetime", "${lifetime.oneDecimal()}x", lifetime, { lifetime = it }, 0.4f..3f)
            ChoiceRow("Shape", ParticleShape.entries, shape, { shape = it }, { it.name })
            LabeledSlider("Seed", "${seed.roundToInt()}", seed, { seed = it }, 0f..20f)
        },
    )
}
