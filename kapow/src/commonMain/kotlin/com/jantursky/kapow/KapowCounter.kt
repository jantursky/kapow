package com.jantursky.kapow

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.text.countValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How [KapowCounter] gets from the old value to the new one. */
public enum class CounterMode {
    /** The number counts through the values in between, formatted again on every frame. */
    Count,
}

/**
 * A number that animates to a new value. The first composition shows [value] at once. When [value] changes, the
 * number moves to it over [duration] and settles on exactly that value. Screen readers always read the new value,
 * never one in between.
 *
 * The counter is as wide as the wider of the old and the new text while it animates, so neighbors do not jiggle,
 * and it is aligned to the end of that width. Progress is read only while drawing, so a value change recomposes the
 * caller once and no frame recomposes anything. A monospaced or tabular-figure font keeps the digits from shifting.
 *
 * With reduced motion the new value appears at once.
 *
 * ```
 * KapowCounter(score, mode = CounterMode.Count, format = { "%,d".format(it) })
 * ```
 *
 * @param value The value to show.
 * @param style Text style. Plain text only.
 * @param mode How the number moves to a new value.
 * @param format Turns a value into the text to show. It is called with the in-between values while counting, so it
 * must be cheap and must not depend on state that changes.
 * @param duration How long the move takes. Must not be negative.
 * @param easing The curve of the move.
 */
@Composable
public fun KapowCounter(
    value: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    mode: CounterMode = CounterMode.Count,
    format: (Long) -> String = { it.toString() },
    duration: Duration = 600.milliseconds,
    easing: Easing = FastOutSlowInEasing,
) {
    require(!duration.isNegative()) { "duration must not be negative" }
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val reduced = isReducedMotion()
    val state = remember { CounterState(value) }
    // Retargeting happens in composition so the first frame of the move is already drawn from the old value.
    state.retarget(value, animate = !reduced && duration.isPositive(), easing = easing)
    val durationMillis = duration.inWholeMilliseconds
    LaunchedEffect(value, durationMillis) {
        if (state.progress >= 1f) return@LaunchedEffect
        val begin = withFrameNanos { it }
        do {
            val elapsed = (withFrameNanos { it } - begin) / 1_000_000f
            state.progress = (elapsed / durationMillis).coerceIn(0f, 1f)
        } while (state.progress < 1f)
        // The move is over: the width goes back to the width of the new text.
        state.from = state.target
    }
    Layout(
        content = {},
        modifier =
            modifier
                .semantics { this.text = AnnotatedString(format(value)) }
                .drawBehind {
                    val width = size.width
                    val shown = state.shown(easing)
                    val layout = measurer.measureNumber(format(shown), style, this, layoutDirection)
                    val x = if (layoutDirection == LayoutDirection.Rtl) 0f else width - layout.size.width
                    drawText(layout, topLeft = Offset(x, 0f))
                },
    ) { _, constraints ->
        val old = measurer.measureNumber(format(state.from), style, this, layoutDirection)
        val new = measurer.measureNumber(format(state.target), style, this, layoutDirection)
        val width = maxOf(old.size.width, new.size.width)
        val height = maxOf(old.size.height, new.size.height)
        layout(
            width.coerceIn(constraints.minWidth, constraints.maxWidth),
            height.coerceIn(constraints.minHeight, constraints.maxHeight),
        ) {
        }
    }
}

private fun TextMeasurer.measureNumber(
    text: String,
    style: TextStyle,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: LayoutDirection,
) = measure(
    text = text,
    style = style,
    softWrap = false,
    maxLines = 1,
    constraints = Constraints(),
    layoutDirection = layoutDirection,
    density = density,
)

/** The value on its way and how far along it is. Read only in the measure and draw phases. */
private class CounterState(
    initial: Long,
) {
    var from by mutableLongStateOf(initial)
    var target by mutableLongStateOf(initial)
    var progress by mutableFloatStateOf(1f)

    /** The value to show now. */
    fun shown(easing: Easing): Long = countValue(from, target, easing.transform(progress))

    fun retarget(
        value: Long,
        animate: Boolean,
        easing: Easing,
    ) = Snapshot.withoutReadObservation {
        if (value == target) return@withoutReadObservation
        // A move in progress goes on from where it is. Not reading state here keeps the caller from recomposing.
        val start = shown(easing)
        if (animate) {
            from = start
            target = value
            progress = 0f
        } else {
            from = value
            target = value
            progress = 1f
        }
    }
}
