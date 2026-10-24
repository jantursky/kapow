package com.jantursky.kapow.samples.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp

/** A 2 dp `hairline` track, an `action` filled part and a 16 dp `action` thumb. */
@Composable
fun KapowSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = KapowTheme.colors
    val currentOnChange by rememberUpdatedState(onValueChange)
    val source = remember { MutableInteractionSource() }
    var widthPx by remember { mutableIntStateOf(0) }
    val span = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(28.dp)
                .kapowFocusRing(source, CircleShape)
                .onSizeChanged { widthPx = it.width }
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
                    setProgress { target ->
                        currentOnChange(target.coerceIn(valueRange.start, valueRange.endInclusive))
                        true
                    }
                }.focusable(interactionSource = source)
                .pointerInput(valueRange, widthPx) {
                    val inset = THUMB_RADIUS.toPx()
                    val usable = (widthPx - 2 * inset).coerceAtLeast(1f)

                    fun valueAt(x: Float) = valueRange.start + ((x - inset) / usable).coerceIn(0f, 1f) * span
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        currentOnChange(valueAt(down.position.x))
                        drag(down.id) {
                            currentOnChange(valueAt(it.position.x))
                            it.consume()
                        }
                    }
                },
    ) {
        val inset = THUMB_RADIUS.toPx()
        val usable = size.width - 2 * inset
        val trackHeight = 2.dp.toPx()
        val centerY = size.height / 2
        val corner = CornerRadius(trackHeight / 2)
        drawRoundRect(colors.hairline, Offset(inset, centerY - trackHeight / 2), Size(usable, trackHeight), corner)
        drawRoundRect(
            colors.action,
            Offset(inset, centerY - trackHeight / 2),
            Size(usable * fraction, trackHeight),
            corner,
        )
        drawCircle(colors.action, inset, Offset(inset + usable * fraction, centerY))
    }
}

private val THUMB_RADIUS = 8.dp
