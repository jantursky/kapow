package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.FlashExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [FlashExample]. */
@Composable
internal fun FlashDemo() {
    var attempts by remember { mutableIntStateOf(0) }
    var swatch by remember { mutableStateOf(Swatch.Coral) }
    var seconds by remember { mutableFloatStateOf(0.4f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { attempts++ },
        stage = { FlashExample(attempts, swatch.color, duration) },
        code = {
            changeEffectCode(
                call("ChangeEffect.flash", "color" to swatch.color.kt(), "duration" to duration.kt()),
                value = "attempts",
            )
        },
        controls = {
            PrimaryPill("Try code", onClick = { attempts++ })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.1f..1.5f)
        },
    )
}
