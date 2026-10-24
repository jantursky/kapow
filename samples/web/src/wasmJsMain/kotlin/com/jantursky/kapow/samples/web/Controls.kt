package com.jantursky.kapow.samples.web

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowFeedbackEvent
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.SegmentedControl
import kotlin.math.roundToInt

/** The effect colors of the design system, as a choice. */
internal enum class Swatch(
    val color: Color,
) {
    Amber(KapowEffectColors.amber),
    Coral(KapowEffectColors.coral),
    Violet(KapowEffectColors.violet),
    Cobalt(KapowEffectColors.cobalt),
    Gray(Color(0xFF8A8A8A)),
    ;

    companion object {
        /** The four effect colors, without the neutral gray that only smoke uses. */
        val Palette: List<Swatch> = listOf(Amber, Coral, Violet, Cobalt)
    }
}

/** The shapes the shape-aware effects accept. */
internal enum class ShapeChoice(
    val shape: Shape,
) {
    Rectangle(RectangleShape),
    Rounded(RoundedCornerShape(16.dp)),
    Circle(CircleShape),
}

/** Where an iris or clock starts. */
internal enum class Origin(
    val origin: TransformOrigin,
) {
    Center(TransformOrigin.Center),
    TopStart(TransformOrigin(0f, 0f)),
    BottomEnd(TransformOrigin(1f, 1f)),
}

/** The feedback events the Feedback page can ask for. */
internal enum class FeedbackChoice(
    val event: KapowFeedbackEvent,
) {
    Select(KapowFeedbackEvent.Selection),
    Impact(KapowFeedbackEvent.ImpactMedium),
    Success(KapowFeedbackEvent.Success),
    Error(KapowFeedbackEvent.Error),
}

/** A label with a segmented control beside it. */
@Composable
internal fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    text: (T) -> String = { it.toString() },
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KapowCaption(label)
        SegmentedControl(options, selected, onSelect, label = text)
    }
}

internal fun Float.oneDecimal(): String = "${(this * 10).roundToInt() / 10f}"

/** Sweeps a progress value from 0 to 1, for the "Play" pill of the progress pages. */
internal suspend fun sweep(update: (Float) -> Unit) {
    animate(0f, 1f, animationSpec = tween(1500)) { value, _ -> update(value) }
}
