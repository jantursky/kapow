package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ShakeAxis
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ShakeExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The controls around [ShakeExample]. */
@Composable
internal fun ShakeDemo() {
    var count by remember { mutableIntStateOf(0) }
    var axis by remember { mutableStateOf(ShakeAxis.Horizontal) }
    var distance by remember { mutableFloatStateOf(8f) }
    var swings by remember { mutableFloatStateOf(4f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var seconds by remember { mutableFloatStateOf(0.5f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    val reach = distance.roundToInt().dp
    ExampleScaffold(
        autoplay = Autoplay(2.4.seconds) { count++ },
        stage = { ShakeExample(count, axis, reach, swings.roundToInt(), rotation, duration) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.shake",
                    *transitionArgs(
                        ("axis" to "ShakeAxis.${axis.name}").takeIf { axis != ShakeAxis.Horizontal },
                        ("distance" to reach.kt()).takeIf { distance.roundToInt() != 8 },
                        ("swings" to swings.roundToInt()).takeIf { swings.roundToInt() != 4 },
                        ("rotation" to rotation.kt()).takeIf { rotation != 0f },
                        ("duration" to duration.kt()).takeIf { seconds != 0.5f },
                    ),
                ),
                value = "attempts",
            )
        },
        controls = {
            PrimaryPill("Wrong password", onClick = { count++ })
            ChoiceRow("Axis", ShakeAxis.entries, axis, { axis = it }, { it.name })
            LabeledSlider("Distance", "${distance.roundToInt()} dp", distance, { distance = it }, 0f..40f)
            LabeledSlider("Swings", "${swings.roundToInt()}", swings, { swings = it }, 1f..20f)
            LabeledSlider("Rotation", "${rotation.roundToInt()}°", rotation, { rotation = it }, 0f..30f)
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..2f)
        },
    )
}
