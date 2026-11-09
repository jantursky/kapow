package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.JumpExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [JumpExample]. */
@Composable
internal fun JumpDemo() {
    var count by remember { mutableIntStateOf(0) }
    var height by remember { mutableFloatStateOf(24f) }
    var hops by remember { mutableFloatStateOf(1f) }
    var squash by remember { mutableFloatStateOf(0.5f) }
    val reach = height.roundToInt().dp
    ExampleScaffold(
        autoplay = Autoplay(2.4.seconds) { count++ },
        stage = { JumpExample(count, reach, hops.roundToInt(), squash, null) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.jump",
                    *transitionArgs(
                        ("height" to reach.kt()).takeIf { height.roundToInt() != 24 },
                        ("hops" to hops.roundToInt()).takeIf { hops.roundToInt() != 1 },
                        ("squash" to squash.kt()).takeIf { squash != 0.5f },
                    ),
                ),
            )
        },
        controls = {
            PrimaryPill("Hop", onClick = { count++ })
            LabeledSlider("Height", "${height.roundToInt()} dp", height, { height = it }, 0f..64f)
            LabeledSlider("Hops", "${hops.roundToInt()}", hops, { hops = it }, 1f..5f)
            LabeledSlider("Squash", squash.oneDecimal(), squash, { squash = it }, 0f..1f)
        },
    )
}
