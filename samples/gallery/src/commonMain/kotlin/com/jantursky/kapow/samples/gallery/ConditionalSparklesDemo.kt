package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ConditionalSparklesExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [ConditionalSparklesExample]. */
@Composable
internal fun ConditionalSparklesDemo() {
    var isNew by remember { mutableStateOf(true) }
    var count by remember { mutableFloatStateOf(4f) }
    var size by remember { mutableFloatStateOf(18f) }
    ExampleScaffold(
        autoplay = Autoplay({ if (isNew) 4.seconds else 1.5.seconds }, startsOn = true) { isNew = !isNew },
        stage = { ConditionalSparklesExample(isNew, count.roundToInt(), size.dp) },
        code = {
            conditionalEffectCode(
                call(
                    "ConditionalEffect.sparkles",
                    "size" to size.dp.kt(),
                    "count" to count.roundToInt(),
                ),
                "isNew",
            )
        },
        controls = {
            PrimaryPill("Toggle", onClick = { isNew = !isNew })
            LabeledSlider("Count", "${count.roundToInt()}", count, { count = it }, 1f..12f)
            LabeledSlider("Size", "${size.roundToInt()} dp", size, { size = it }, 8f..32f)
        },
    )
}
