package com.jantursky.kapow.samples.gallery

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ScrollingHeartsExample
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/** The controls around [ScrollingHeartsExample]. */
@Composable
internal fun ScrollingHeartsDemo() {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var likes by remember { mutableIntStateOf(0) }
    ExampleScaffold(
        autoplay =
            Autoplay(3.seconds) {
                likes++
                scope.launch { state.animateScrollBy(if (likes % 2 == 0) -120f else 120f) }
            },
        stage = { ScrollingHeartsExample(state, likes, likedRow = 1) },
        code = {
            "KapowParticleLayer {\n    LazyColumn {\n        items(rows) { row ->\n" +
                "            Box(Modifier.changeEffect(likes[row], spray)) { /* Like */ }\n" +
                "        }\n    }\n}\n// The hearts scroll with their row."
        },
        controls = {
            PrimaryPill("Like", onClick = { likes++ })
            PrimaryPill("Scroll", onClick = { scope.launch { state.animateScrollBy(180f) } })
        },
    )
}
