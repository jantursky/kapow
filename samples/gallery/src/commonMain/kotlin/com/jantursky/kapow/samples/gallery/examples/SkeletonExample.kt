package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.kapowSkeleton
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

private val Ink = Color(0xFF161616)

/**
 * A profile card whose avatar and two lines of text are covered by skeletons while [loading]. The real content is
 * laid out all the time, so the card does not change size when the data arrives.
 */
@Composable
fun SkeletonExample(
    loading: Boolean,
    shimmer: Boolean,
    revealDuration: Duration,
) {
    Row(
        Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3F4F8))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .kapowSkeleton(loading, CircleShape, shimmer = shimmer, revealDuration = revealDuration)
                .background(KapowEffectColors.violet, CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KapowText(
                "Maya Okafor",
                Modifier.kapowSkeleton(loading, shimmer = shimmer, revealDuration = revealDuration),
                color = Ink,
            )
            KapowText(
                "Design engineer",
                Modifier.kapowSkeleton(loading, shimmer = shimmer, revealDuration = revealDuration),
                color = Ink.copy(alpha = 0.6f),
            )
        }
    }
}
