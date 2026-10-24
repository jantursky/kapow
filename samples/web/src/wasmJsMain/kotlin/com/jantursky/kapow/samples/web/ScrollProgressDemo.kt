package com.jantursky.kapow.samples.web

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.ScrollProgressExample
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/** The controls around [ScrollProgressExample]. */
@Composable
internal fun ScrollProgressDemo() {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var glow by remember { mutableStateOf(false) }
    val scroll: suspend () -> Unit = {
        scrollState.animateScrollTo(if (scrollState.value == 0) scrollState.maxValue else 0)
    }
    ExampleScaffold(
        autoplay = Autoplay(1.seconds, scroll),
        stage = { ScrollProgressExample(scrollState, glow) },
        controls = {
            PrimaryPill("Scroll", onClick = { scope.launch { scroll() } })
            ChoiceRow("Effect", listOf(false, true), glow, { glow = it }, { if (it) "Glow" else "Shine" })
        },
    )
}
