package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.KapowToggle
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ProgressIrisExample
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [ProgressIrisExample]. */
@Composable
internal fun ProgressIrisDemo() {
    var progress by remember { mutableFloatStateOf(0f) }
    var origin by remember { mutableStateOf(Origin.Center) }
    var soft by remember { mutableFloatStateOf(0f) }
    var square by remember { mutableStateOf(false) }
    var invert by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ExampleScaffold(
        autoplay = Autoplay(1.seconds) { sweepThereAndBack { progress = it } },
        stage = { ProgressIrisExample(progress, origin.origin, soft.dp, if (square) RectangleShape else null, invert) },
        code = {
            val args =
                listOfNotNull(
                    "origin" to origin.source(),
                    ("softEdge" to soft.dp.kt()).takeIf { soft != 0f },
                    ("shape" to "RectangleShape").takeIf { square },
                    ("invert" to true).takeIf { invert },
                )
            progressEffectCode(call("ProgressEffect.iris", *args.toTypedArray()))
        },
        controls = {
            PrimaryPill("Play", onClick = { scope.launch { sweep { progress = it } } })
            LabeledSlider("Progress", "${(progress * 100).roundToInt()} %", progress, { progress = it }, 0f..1f)
            ChoiceRow("Origin", Origin.entries, origin, { origin = it }, { it.name })
            LabeledSlider("Soft edge", "${soft.roundToInt()} dp", soft, { soft = it }, 0f..60f)
            KapowToggle(square, { square = it }, "Square shape")
            KapowToggle(invert, { invert = it }, "Invert")
        },
    )
}

/** The origin as the code a person would write. */
private fun Origin.source(): String =
    when (this) {
        Origin.Center -> "TransformOrigin.Center"
        Origin.TopStart -> "TransformOrigin(0f, 0f)"
        Origin.BottomEnd -> "TransformOrigin(1f, 1f)"
    }
