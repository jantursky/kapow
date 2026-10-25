package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.samples.design.KapowToggle
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.TiltExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [TiltExample]. The tilt itself follows the pointer over the card. */
@Composable
internal fun TiltDemo() {
    var taps by remember { mutableIntStateOf(0) }
    var swatch by remember { mutableStateOf(Swatch.Violet) }
    var angle by remember { mutableFloatStateOf(12f) }
    var sheen by remember { mutableStateOf(true) }
    var enabled by remember { mutableStateOf(true) }
    ExampleScaffold(
        // A pointer cannot be played back, so autoplay taps the card to show that it stays clickable.
        autoplay = Autoplay(3.seconds) { taps++ },
        stage = { TiltExample(taps, { taps++ }, swatch.color, angle, sheen, enabled) },
        code = {
            call(
                "Modifier.kapowTilt",
                "maxAngle" to angle.roundToInt().toFloat().kt(),
                "sheen" to if (sheen) "Color.White.copy(alpha = 0.4f)" else "null",
                "enabled" to "$enabled",
            ).render()
        },
        controls = {
            PrimaryPill("Tap the card", onClick = { taps++ })
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Max angle", "${angle.roundToInt()} deg", angle, { angle = it }, 0f..30f)
            KapowToggle(sheen, { sheen = it }, "Sheen")
            KapowToggle(enabled, { enabled = it }, "Enabled")
        },
    )
}
