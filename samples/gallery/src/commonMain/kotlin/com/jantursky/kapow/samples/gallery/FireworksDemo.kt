package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.jantursky.kapow.ParticleContent
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.FireworksExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The spark colors the page offers: all four effect colors, or a warm or a cool pair. */
private enum class SparkColors(
    val colors: List<Color>,
) {
    Mixed(ParticleContent.DefaultColors),
    Warm(listOf(KapowEffectColors.amber, KapowEffectColors.coral)),
    Cool(listOf(KapowEffectColors.violet, KapowEffectColors.cobalt)),
}

/** The controls around [FireworksExample]. */
@Composable
internal fun FireworksDemo() {
    var days by remember { mutableIntStateOf(6) }
    var palette by remember { mutableStateOf(SparkColors.Mixed) }
    var sparks by remember { mutableFloatStateOf(28f) }
    ExampleScaffold(
        autoplay = Autoplay(2.2.seconds) { days++ },
        stage = { FireworksExample(days, palette.colors, sparks.roundToInt()) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.fireworks",
                    *listOfNotNull(
                        palette.kt()?.let { "colors" to it },
                        "count" to sparks.roundToInt().toString(),
                    ).toTypedArray(),
                ),
                value = "days",
            )
        },
        controls = {
            PrimaryPill("Check in", onClick = { days++ })
            ChoiceRow("Colors", SparkColors.entries, palette, { palette = it }, { it.name })
            LabeledSlider("Sparks", sparks.roundToInt().toString(), sparks, { sparks = it }, 8f..60f)
        },
    )
}

/** The colors as the source a user would write; the mixed default needs no argument. */
private fun SparkColors.kt(): String? =
    if (this == SparkColors.Mixed) null else "listOf(${colors.joinToString { it.kt() }})"
