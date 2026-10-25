package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.FillDirection
import com.jantursky.kapow.samples.design.KapowToggle
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ProgressFillExample
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The colors the liquid ramps through, from progress 0 to 1. */
private val FILL_RAMP = listOf(Color(0x99FF6B5A), Color(0x99FFB547), Color(0x993DD6C6))

/** The controls around [ProgressFillExample]. */
@Composable
internal fun ProgressFillDemo() {
    var progress by remember { mutableFloatStateOf(0f) }
    var swatch by remember { mutableStateOf(Swatch.Cobalt) }
    var wave by remember { mutableFloatStateOf(4f) }
    var direction by remember { mutableStateOf(FillDirection.Up) }
    var waves by remember { mutableFloatStateOf(1.5f) }
    var foam by remember { mutableFloatStateOf(0f) }
    var ramp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ExampleScaffold(
        autoplay = Autoplay(1.seconds) { sweepThereAndBack { progress = it } },
        stage = {
            ProgressFillExample(
                progress,
                swatch.color,
                wave.dp,
                direction,
                waves,
                foam.dp,
                if (ramp) FILL_RAMP else null,
            )
        },
        code = {
            val args =
                listOfNotNull(
                    "color" to "${swatch.color.kt()}.copy(alpha = 0.6f)",
                    "wave" to wave.dp.kt(),
                    ("direction" to "FillDirection.${direction.name}").takeIf { direction != FillDirection.Up },
                    ("waves" to waves.kt()).takeIf { waves != 1.5f },
                    ("foam" to foam.dp.kt()).takeIf { foam != 0f },
                    ("colorRamp" to "listOf(${FILL_RAMP.joinToString { it.kt() }})").takeIf { ramp },
                )
            progressEffectCode(call("ProgressEffect.fill", *args.toTypedArray()))
        },
        controls = {
            PrimaryPill("Play", onClick = { scope.launch { sweep { progress = it } } })
            LabeledSlider("Progress", "${(progress * 100).roundToInt()} %", progress, { progress = it }, 0f..1f)
            ChoiceRow("Color", Swatch.Palette, swatch, { swatch = it }, { it.name })
            LabeledSlider("Wave", "${wave.roundToInt()} dp", wave, { wave = it }, 0f..12f)
            ChoiceRow("Direction", FillDirection.entries, direction, { direction = it }, { it.name })
            LabeledSlider("Waves", waves.oneDecimal(), waves, { waves = it }, 0.5f..4f)
            LabeledSlider("Foam", "${foam.roundToInt()} dp", foam, { foam = it }, 0f..8f)
            KapowToggle(ramp, { ramp = it }, "Color ramp")
        },
    )
}
