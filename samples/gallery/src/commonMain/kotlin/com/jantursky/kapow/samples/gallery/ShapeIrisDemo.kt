package com.jantursky.kapow.samples.gallery

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.StarShape
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ShapeIrisExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The openings the shape iris page offers. */
private enum class Opening(
    val shape: Shape,
    val code: String,
) {
    Star(StarShape(), "StarShape()"),
    Burst(StarShape(points = 12, innerRatio = 0.6f), "StarShape(points = 12, innerRatio = 0.6f)"),
    Circle(CircleShape, "CircleShape"),
    Rounded(RoundedCornerShape(24.dp), "RoundedCornerShape(24.dp)"),
}

/** The controls around [ShapeIrisExample]. */
@Composable
internal fun ShapeIrisDemo() {
    var visible by remember { mutableStateOf(false) }
    var opening by remember { mutableStateOf(Opening.Star) }
    var origin by remember { mutableStateOf(Origin.Center) }
    var seconds by remember { mutableFloatStateOf(0.6f) }
    val duration = (seconds * 1000).roundToInt().milliseconds
    ExampleScaffold(
        autoplay = Autoplay(duration + 1.2.seconds) { visible = !visible },
        stage = { ShapeIrisExample(visible, opening.shape, origin.origin, duration) },
        code = {
            val args = arrayOf("shape" to opening.code, "origin" to origin.kt(), "duration" to duration.kt())
            transitionCode(call("KapowEnter.shapeIris", *args), call("KapowExit.shapeIris", *args))
        },
        controls = {
            PrimaryPill(if (visible) "Hide" else "Show", onClick = { visible = !visible })
            ChoiceRow("Shape", Opening.entries, opening, { opening = it }, { it.name })
            ChoiceRow("Origin", Origin.entries, origin, { origin = it }, { it.name })
            LabeledSlider("Duration", "${seconds.oneDecimal()} s", seconds, { seconds = it }, 0.2f..3f)
        },
    )
}
