package com.jantursky.kapow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.text.cursorVisible
import com.jantursky.kapow.text.totalMillis
import com.jantursky.kapow.text.visibleCount
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How [KapowTypewriter] reveals its text. */
public enum class TypewriterMode {
    /** Characters appear one after another from the start. */
    Type,
}

/** The cursor [KapowTypewriter] draws after the last visible character. Create one with [bar]. */
public class TypewriterCursor internal constructor(
    /** Color of the bar. [Color.Unspecified] uses the text color. */
    public val color: Color,
    /** Thickness of the bar. */
    public val width: Dp,
    /** How long the bar stays on, and then off, once the text is complete. */
    public val blink: Duration,
    /** Whether the bar goes away when the text is complete instead of blinking on. */
    public val hideWhenDone: Boolean,
) {
    override fun equals(other: Any?): Boolean =
        other is TypewriterCursor &&
            other.color == color &&
            other.width == width &&
            other.blink == blink &&
            other.hideWhenDone == hideWhenDone

    override fun hashCode(): Int =
        ((color.hashCode() * 31 + width.hashCode()) * 31 + blink.hashCode()) * 31 + hideWhenDone.hashCode()

    override fun toString(): String =
        "TypewriterCursor.bar(color=$color, width=$width, blink=$blink, hideWhenDone=$hideWhenDone)"

    public companion object {
        /**
         * A vertical bar the height of the line. It stays solid while characters appear and then blinks, or goes
         * away when [hideWhenDone] is true. With reduced motion it is not drawn.
         *
         * @param blink How long the bar stays on and then off. Must be positive.
         */
        public fun bar(
            color: Color = Color.Unspecified,
            width: Dp = 2.dp,
            blink: Duration = 530.milliseconds,
            hideWhenDone: Boolean = false,
        ): TypewriterCursor {
            require(width.value > 0f) { "width must be positive" }
            require(blink.isPositive()) { "blink must be positive" }
            return TypewriterCursor(color, width, blink, hideWhenDone)
        }
    }
}

/**
 * Text that reveals itself over time. It measures the full [text] first and sizes itself to it, so it takes its
 * final space from the first frame and nothing around it moves while it types. The reveal restarts whenever
 * [text] or any timing parameter changes. Screen readers always read the full text.
 *
 * Progress is read only while drawing, so the reveal does not recompose anything.
 *
 * With reduced motion the full text shows at once, without a cursor.
 *
 * ```
 * KapowTypewriter("Hello, Kapow.", cursor = TypewriterCursor.bar())
 * ```
 *
 * @param style Text style. Plain text only: no spans and no per-character styling.
 * @param mode How the text is revealed.
 * @param charsPerSecond How many characters appear per second. Must be positive.
 * @param startDelay How long to wait before the first character. Must not be negative.
 * @param cursor The cursor to draw after the last visible character, or null for none.
 * @param punctuationPause Extra pause after each of `, . ; : ! ?`. Must not be negative.
 */
@Composable
public fun KapowTypewriter(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    mode: TypewriterMode = TypewriterMode.Type,
    charsPerSecond: Float = 30f,
    startDelay: Duration = Duration.ZERO,
    cursor: TypewriterCursor? = null,
    punctuationPause: Duration = Duration.ZERO,
) {
    require(charsPerSecond > 0f && charsPerSecond.isFinite()) { "charsPerSecond must be positive" }
    require(!startDelay.isNegative()) { "startDelay must not be negative" }
    require(!punctuationPause.isNegative()) { "punctuationPause must not be negative" }
    val measurer = rememberTextMeasurer()
    val reduced = isReducedMotion()
    val measured = remember { MeasuredText() }
    val startMillis = startDelay.inWholeMilliseconds
    val pauseMillis = punctuationPause.inWholeMilliseconds
    val total = totalMillis(text, charsPerSecond, startMillis, pauseMillis)
    // A clock of its own per reveal, so a new text never draws a frame at the old text's progress.
    val clock = remember(text, mode, charsPerSecond, startMillis, pauseMillis, reduced) { mutableLongStateOf(0L) }
    val blinking = cursor != null && !cursor.hideWhenDone
    LaunchedEffect(clock, blinking) {
        if (reduced) return@LaunchedEffect
        val begin = withFrameNanos { it }
        do {
            clock.longValue = (withFrameNanos { it } - begin) / 1_000_000
        } while (blinking || clock.longValue < total)
    }
    Layout(
        content = {},
        modifier =
            modifier
                .semantics { this.text = AnnotatedString(text) }
                .drawBehind {
                    val layout = measured.layout ?: return@drawBehind
                    val elapsed = clock.longValue
                    val count =
                        if (reduced) {
                            text.length
                        } else {
                            visibleCount(
                                text,
                                elapsed,
                                charsPerSecond,
                                startMillis,
                                pauseMillis,
                            )
                        }
                    drawReveal(layout, count)
                    if (cursor != null && !reduced) {
                        val done = elapsed >= total
                        if (!(done && cursor.hideWhenDone) &&
                            cursorVisible(elapsed, total, cursor.blink.inWholeMilliseconds.coerceAtLeast(1))
                        ) {
                            drawCursor(layout, count, cursor, style.color)
                        }
                    }
                },
    ) { _, constraints ->
        val result =
            measurer.measure(
                text = text,
                style = style,
                constraints = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight),
                layoutDirection = layoutDirection,
                density = this,
            )
        measured.layout = result
        layout(
            result.size.width.coerceIn(constraints.minWidth, constraints.maxWidth),
            result.size.height.coerceIn(constraints.minHeight, constraints.maxHeight),
        ) {}
    }
}

/** The layout of the full text, set when measuring and read when drawing. */
private class MeasuredText {
    var layout: TextLayoutResult? = null
}

/** Draws the first [count] characters of [layout] at the places they have in the full text. */
private fun DrawScope.drawReveal(
    layout: TextLayoutResult,
    count: Int,
) {
    val length = layout.layoutInput.text.length
    when {
        count >= length -> drawText(layout)
        count > 0 -> clipPath(layout.getPathForRange(0, count)) { drawText(layout) }
    }
}

private fun DrawScope.drawCursor(
    layout: TextLayoutResult,
    count: Int,
    cursor: TypewriterCursor,
    textColor: Color,
) {
    val rect = layout.getCursorRect(count.coerceIn(0, layout.layoutInput.text.length))
    val color =
        if (cursor.color.isSpecified) {
            cursor.color
        } else if (textColor.isSpecified) {
            textColor
        } else {
            Color.Black
        }
    val width = cursor.width.toPx()
    drawRect(color, Offset(rect.left, rect.top), Size(width, rect.height))
}

private val Color.isSpecified: Boolean get() = this != Color.Unspecified
