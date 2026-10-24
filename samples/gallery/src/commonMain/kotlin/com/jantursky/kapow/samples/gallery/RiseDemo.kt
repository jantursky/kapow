package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.RiseExample
import kotlin.time.Duration.Companion.seconds

/** The controls around [RiseExample]. */
@Composable
internal fun RiseDemo() {
    var items by remember { mutableIntStateOf(1) }
    var label by remember { mutableStateOf("+1") }
    ExampleScaffold(
        autoplay = Autoplay(2.seconds) { items++ },
        stage = { RiseExample(items, label) },
        code = {
            changeEffectCode(
                call("ChangeEffect.rise", "" to call("ParticleContent.text", "" to "\"$label\"")),
                value = "items",
            )
        },
        controls = {
            PrimaryPill("Add to cart", onClick = { items++ })
            ChoiceRow("Label", listOf("+1", "+5", "New"), label, { label = it })
        },
    )
}
