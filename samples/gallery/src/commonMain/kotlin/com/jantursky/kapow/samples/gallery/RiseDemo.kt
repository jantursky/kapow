package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.RiseExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [RiseExample]. */
@Composable
internal fun RiseDemo() {
    var items by remember { mutableIntStateOf(1) }
    var label by remember { mutableStateOf("+1") }
    var distance by remember { mutableFloatStateOf(44f) }
    var drift by remember { mutableFloatStateOf(0f) }
    ExampleScaffold(
        autoplay = Autoplay(2.seconds) { items++ },
        stage = { RiseExample(items, label, distance.dp, drift) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.rise",
                    "" to call("ParticleContent.text", "" to "\"$label\""),
                    "distance" to "${distance.roundToInt()}.dp",
                    "drift" to "${drift.roundToInt()}f",
                ),
                value = "items",
            )
        },
        controls = {
            PrimaryPill("Add to cart", onClick = { items++ })
            ChoiceRow("Label", listOf("+1", "+5", "New"), label, { label = it })
            LabeledSlider("Distance", "${distance.roundToInt()} dp", distance, { distance = it }, 20f..120f)
            LabeledSlider("Drift", "${drift.roundToInt()}°", drift, { drift = it }, -60f..60f)
        },
    )
}
