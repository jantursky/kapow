package com.jantursky.kapow.samples.gallery

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.KapowInteraction
import com.jantursky.kapow.samples.gallery.examples.InteractionExample
import kotlin.time.Duration.Companion.seconds

/** The controls around [InteractionExample]. */
@Composable
internal fun InteractionDemo() {
    var interaction by remember { mutableStateOf(KapowInteraction.Hovered) }
    val source = remember(interaction) { MutableInteractionSource() }
    ExampleScaffold(
        autoplay = Autoplay(4.seconds) {},
        stage = { InteractionExample(interaction, source) },
        code = {
            "val source = remember { MutableInteractionSource() }\n\n" +
                call(
                    "Modifier.conditionalEffect",
                    "" to "source",
                    "" to "KapowInteraction.${interaction.name}",
                    "" to call("ConditionalEffect.glow", "" to "Color.Magenta"),
                ).render() +
                "\n    .hoverable(source)\n    .focusable(interactionSource = source)\n" +
                "    .clickable(interactionSource = source, indication = null) { }"
        },
        controls = {
            ChoiceRow("While", KapowInteraction.entries, interaction, { interaction = it }, { it.name })
        },
    )
}
