package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.FeedbackExample
import kotlin.time.Duration.Companion.seconds

/** The controls around [FeedbackExample]. */
@Composable
internal fun FeedbackDemo() {
    var count by remember { mutableIntStateOf(0) }
    var choice by remember { mutableStateOf(FeedbackChoice.Success) }
    var last by remember { mutableStateOf("none yet") }
    ExampleScaffold(
        autoplay = Autoplay(2.2.seconds) { count++ },
        stage = { FeedbackExample(count, choice.event, { last = it.toString() }) },
        controls = {
            PrimaryPill("Fire", onClick = { count++ })
            ChoiceRow("Event", FeedbackChoice.entries, choice, { choice = it }, { it.name })
            KapowCaption("Handler received: $last")
        },
    )
}
