package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.rememberKapowTrigger
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.TriggerExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [TriggerExample]. */
@Composable
internal fun TriggerDemo() {
    val trigger = rememberKapowTrigger()
    var count by remember { mutableFloatStateOf(11f) }
    ExampleScaffold(
        autoplay = Autoplay(2.seconds) { trigger.fire() },
        stage = { TriggerExample(trigger, count.roundToInt()) },
        code = {
            "val trigger = rememberKapowTrigger()\n\n" +
                call(
                    "Modifier.changeEffect",
                    "" to "trigger",
                    "" to
                        call(
                            "ChangeEffect.spray",
                            "" to "ParticleContent.shape(ParticleShape.Heart)",
                            "count" to count.roundToInt().toString(),
                        ),
                ).render() +
                "\n\n// anywhere: onClick = { trigger.fire() }"
        },
        controls = {
            PrimaryPill("Fire from code", onClick = { trigger.fire() })
            LabeledSlider("Count", count.roundToInt().toString(), count, { count = it }, 3f..24f)
        },
    )
}
