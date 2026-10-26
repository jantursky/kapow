package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.KapowToggle
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.SkeletonExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [SkeletonExample]. */
@Composable
internal fun SkeletonDemo() {
    var loading by remember { mutableStateOf(true) }
    var shimmer by remember { mutableStateOf(true) }
    var reveal by remember { mutableFloatStateOf(300f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (loading) 3.seconds else 2.seconds }, startsOn = true) { loading = !loading },
        stage = { SkeletonExample(loading, shimmer, reveal.roundToInt().milliseconds) },
        code = {
            val args =
                listOfNotNull(
                    "" to "loading",
                    ("shimmer" to "false").takeIf { !shimmer },
                    ("revealDuration" to reveal.roundToInt().milliseconds.kt()).takeIf { reveal.roundToInt() != 300 },
                )
            call("Modifier.kapowSkeleton", *args.toTypedArray()).render()
        },
        controls = {
            PrimaryPill("Toggle", onClick = { loading = !loading })
            KapowToggle(shimmer, { shimmer = it }, "Shimmer")
            LabeledSlider("Reveal", "${reveal.roundToInt()} ms", reveal, { reveal = it }, 0f..1000f)
        },
    )
}
