package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.TypewriterMode
import com.jantursky.kapow.samples.design.KapowToggle
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.TypewriterExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val lines =
    listOf(
        "Hello, Kapow. Effects that feel alive.",
        "Short and sweet: no jumps, no flicker.",
        "Type, pause, and carry on. Done!",
    )

/** The controls around [TypewriterExample]. */
@Composable
internal fun TypewriterDemo() {
    var index by remember { mutableIntStateOf(0) }
    var rate by remember { mutableFloatStateOf(24f) }
    var mode by remember { mutableStateOf(TypewriterMode.Type) }
    var cursor by remember { mutableStateOf(true) }
    var pause by remember { mutableStateOf(false) }
    val text = lines[index]
    val perChar = (1000f / rate).roundToInt()
    ExampleScaffold(
        autoplay = Autoplay({ (text.length * perChar).milliseconds + 2.seconds }) { index = (index + 1) % lines.size },
        stage = { TypewriterExample(text, rate, cursor, mode, if (pause) 300.milliseconds else 0.milliseconds) },
        code = {
            call(
                "KapowTypewriter",
                "text" to "\"...\"",
                "mode" to "TypewriterMode.${mode.name}",
                "charsPerSecond" to rate.roundToInt().toFloat().kt(),
                "cursor" to if (cursor) "TypewriterCursor.bar()" else "null",
            ).render()
        },
        controls = {
            PrimaryPill("Next line", onClick = { index = (index + 1) % lines.size })
            ChoiceRow("Mode", TypewriterMode.entries, mode, { mode = it }, { it.name })
            LabeledSlider("Speed", "${rate.roundToInt()} chars/s", rate, { rate = it }, 5f..60f)
            KapowToggle(cursor, { cursor = it }, "Cursor")
            KapowToggle(pause, { pause = it }, "Pause on punctuation")
        },
    )
}
