package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.KapowFeedback
import com.jantursky.kapow.KapowFeedbackEvent
import com.jantursky.kapow.LocalKapowFeedback
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText

/**
 * A button whose shine asks for [event] feedback. Here the handler reports the event to [onFeedback]; an app
 * would map it to haptics or sound, or use the kapow-multihaptic adapter.
 */
@Composable
fun FeedbackExample(
    count: Int,
    event: KapowFeedbackEvent,
    onFeedback: (KapowFeedbackEvent) -> Unit,
) {
    CompositionLocalProvider(LocalKapowFeedback provides KapowFeedback { onFeedback(it) }) {
        Box(
            Modifier
                .changeEffect(count, ChangeEffect.shine(), feedback = event)
                .clip(KapowShapes.pill)
                .background(KapowEffectColors.amber)
                .padding(horizontal = 40.dp, vertical = 16.dp),
        ) {
            KapowText("Confirm", color = Color.Black)
        }
    }
}
