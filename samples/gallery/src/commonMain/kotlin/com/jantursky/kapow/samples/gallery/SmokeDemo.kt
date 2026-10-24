package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.SmokeExample
import kotlin.time.Duration.Companion.seconds

/** The controls around [SmokeExample]. */
@Composable
internal fun SmokeDemo() {
    var hot by remember { mutableStateOf(true) }
    var swatch by remember { mutableStateOf(Swatch.Gray) }
    ExampleScaffold(
        autoplay = Autoplay({ if (hot) 4.seconds else 2.seconds }, startsOn = true) { hot = !hot },
        stage = { SmokeExample(hot, swatch.color) },
        code = {
            conditionalEffectCode(
                call(
                    "ConditionalEffect.smoke",
                    "color" to swatch.color.kt(),
                ),
                "hot",
            )
        },
        controls = {
            PrimaryPill("Toggle", onClick = { hot = !hot })
            ChoiceRow(
                "Color",
                listOf(Swatch.Gray, Swatch.Amber, Swatch.Coral, Swatch.Cobalt),
                swatch,
                { swatch = it },
                { it.name },
            )
        },
    )
}
