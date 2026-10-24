package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.FeedbackExample
import kotlin.time.Duration.Companion.seconds

/** The name of the event as a user writes it. */
private val FeedbackChoice.sourceName: String
    get() =
        when (this) {
            FeedbackChoice.Select -> "Selection"
            FeedbackChoice.Impact -> "ImpactMedium"
            FeedbackChoice.Success -> "Success"
            FeedbackChoice.Error -> "Error"
        }

/** The controls around [FeedbackExample]. */
@Composable
internal fun FeedbackDemo() {
    var count by remember { mutableIntStateOf(0) }
    var choice by remember { mutableStateOf(FeedbackChoice.Success) }
    var last by remember { mutableStateOf("none yet") }
    ExampleScaffold(
        autoplay = Autoplay(2.2.seconds) { count++ },
        stage = { FeedbackExample(count, choice.event, { last = it.toString() }) },
        code = {
            val feedback = "KapowFeedbackEvent.${choice.sourceName}"
            "CompositionLocalProvider(LocalKapowFeedback provides KapowFeedback { event -> /* haptics */ }) {\n" +
                "    Box(\n        Modifier.changeEffect(count, ChangeEffect.shine(), feedback = $feedback),\n" +
                "    ) {\n        // your content\n    }\n}"
        },
        controls = {
            PrimaryPill("Fire", onClick = { count++ })
            ChoiceRow("Event", FeedbackChoice.entries, choice, { choice = it }, { it.name })
            KapowCaption("Handler received: $last")
        },
    )
}
