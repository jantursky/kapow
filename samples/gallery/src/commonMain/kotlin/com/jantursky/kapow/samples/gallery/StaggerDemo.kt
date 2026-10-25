package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.GhostPill
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.StaggerExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [StaggerExample]. */
@Composable
internal fun StaggerDemo() {
    var visible by remember { mutableStateOf(false) }
    var shine by remember { mutableIntStateOf(0) }
    var stepMillis by remember { mutableFloatStateOf(80f) }
    val step = stepMillis.roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(step * 5 + 1.5.seconds) { visible = !visible },
        stage = { StaggerExample(visible, shine, step) },
        code = {
            """
            items.forEachIndexed { index, item ->
                val delay = KapowStagger.delay(index, step = ${step.kt()})
                KapowAnimatedVisibility(
                    visible,
                    enter = KapowEnter.wipe().delay(delay),
                    exit = KapowExit.wipe().delay(delay),
                ) {
                    Row(Modifier.changeEffect(shine, ChangeEffect.shine().delay(delay))) { /* ... */ }
                }
            }
            """.trimIndent()
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            GhostPill("Shine", onClick = { shine++ })
            LabeledSlider("Step", "${stepMillis.roundToInt()} ms", stepMillis, { stepMillis = it }, 0f..200f)
        },
    )
}
