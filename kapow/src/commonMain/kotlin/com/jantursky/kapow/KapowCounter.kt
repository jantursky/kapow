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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.text.SlotKind
import com.jantursky.kapow.text.countValue
import com.jantursky.kapow.text.rollDirection
import com.jantursky.kapow.text.rollSlots
import com.jantursky.kapow.text.slotProgress
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How [KapowCounter] gets from the old value to the new one. */
public enum class CounterMode {
    /** The number counts through the values in between, formatted again on every frame. */
    Count,

    /**
     * Each digit has a slot, aligned on the last digit. A digit that changes rolls out of its slot and the new one
     * rolls in, upward when the value goes up and downward when it goes down, with the slots starting one after the
     * other from the right. Anything that is not a digit changing to a digit, such as a separator or a digit that
     * appears or goes away, cross-fades. Slots that do not change stay still.
     */
    Roll,
}

/**
 * A number that animates to a new value. The first composition shows [value] at once. When [value] changes, the
 * number rolls or counts to it over [duration] and settles on exactly that value. Screen readers always read the new value,
 * never one in between.
 *
 * The counter is as wide as the wider of the old and the new text while it animates, so neighbors do not jiggle,
 * and it is aligned to the end of that width. Progress is read only while drawing, so a value change recomposes the
 * caller once and no frame recomposes anything. A monospaced or tabular-figure font keeps the digits from shifting.
 *
 * With reduced motion the new value appears at once.
 *
 * ```
 * KapowCounter(score, mode = CounterMode.Roll, format = { "$it pts" })
 * ```
 *
 * @param value The value to show.
 * @param style Text style. Plain text only.
 * @param mode How the number moves to a new value.
 * @param format Turns a value into the text to show. It is called with the in-between values in
 * [CounterMode.Count], and with the old and the new value in [CounterMode.Roll]. It must be cheap and must not depend
 * on state that changes.
 * @param duration How long the move takes. Must not be negative.
 * @param easing The curve of the move. In [CounterMode.Roll] it applies to each slot.
 */
@Composable
public fun KapowCounter(
    value: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    mode: CounterMode = CounterMode.Roll,
    format: (Long) -> String = { it.toString() },
    duration: Duration = 600.milliseconds,
    easing: Easing = FastOutSlowInEasing,
) {
    require(!duration.isNegative()) { "duration must not be negative" }
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val reduced = isReducedMotion()
    val state = remember { CounterState(value) }
    // Retargeting happens in composition so the first frame of the move is already drawn from the old value.
    state.retarget(value, animate = !reduced && duration.isPositive(), easing = easing, mode = mode)
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
                    if (mode == CounterMode.Roll && state.from != state.target) {
                        drawRolling(state, format, style, easing, measurer)
                    } else {
                        val shown = state.shown(easing)
                        val layout = measurer.measureNumber(format(shown), style, this, layoutDirection)
                        val x = if (layoutDirection == LayoutDirection.Rtl) 0f else size.width - layout.size.width
                        drawText(layout, topLeft = Offset(x, 0f))
                    }
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

/** How much of the duration goes to starting the slots one after the other. */
private const val SLOT_STAGGER = 0.35f

/** Draws the rolling counter: every slot between the old and the new text, aligned on the last character. */
private fun DrawScope.drawRolling(
    state: CounterState,
    format: (Long) -> String,
    style: TextStyle,
    easing: Easing,
    measurer: TextMeasurer,
) {
    val old = measurer.measureNumber(format(state.from), style, this, layoutDirection)
    val new = measurer.measureNumber(format(state.target), style, this, layoutDirection)
    val slots = rollSlots(old.layoutInput.text.text, new.layoutInput.text.text)
    val direction = rollDirection(state.from, state.target)
    val oldX = size.width - old.size.width
    val newX = size.width - new.size.width
    val oldStart = slots.size - old.layoutInput.text.length
    val newStart = slots.size - new.layoutInput.text.length
    slots.forEachIndexed { slot, pair ->
        val e = easing.transform(slotProgress(state.progress, slots.size - 1 - slot, slots.size, SLOT_STAGGER))
        val oldIndex = slot - oldStart
        val newIndex = slot - newStart
        when (pair.kind) {
            SlotKind.Static -> {
                drawGlyph(new, newIndex, newX, 0f, 0f, 1f)
            }

            SlotKind.Roll -> {
                val travel = size.height * direction
                // The digits can differ in width, so the old one slides toward the place of the new one.
                val shift = (newX + new.getBoundingBox(newIndex).left) - (oldX + old.getBoundingBox(oldIndex).left)
                drawGlyph(old, oldIndex, oldX, shift * e, -travel * e, 1f - e)
                drawGlyph(new, newIndex, newX, -shift * (1f - e), travel * (1f - e), e)
            }

            SlotKind.Fade -> {
                if (pair.from != null) drawGlyph(old, oldIndex, oldX, 0f, 0f, 1f - e)
                if (pair.to != null) drawGlyph(new, newIndex, newX, 0f, 0f, e)
            }
        }
    }
}

/**
 * Draws the character [index] of [layout], whose text starts at [originX], moved by [dx] and [dy]. Only the part
 * inside the character's own box shows, and nothing outside the counter's height.
 */
private fun DrawScope.drawGlyph(
    layout: TextLayoutResult,
    index: Int,
    originX: Float,
    dx: Float,
    dy: Float,
    alpha: Float,
) {
    val box = layout.getBoundingBox(index)
    clipRect(originX + dx + box.left, 0f, originX + dx + box.right, size.height) {
        translate(originX + dx, dy) { drawText(layout, alpha = alpha) }
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
        mode: CounterMode,
    ) = Snapshot.withoutReadObservation {
        if (value == target) return@withoutReadObservation
        // A move in progress goes on from where it is. Not reading state here keeps the caller from recomposing.
        val start = if (mode == CounterMode.Roll) target else shown(easing)
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
