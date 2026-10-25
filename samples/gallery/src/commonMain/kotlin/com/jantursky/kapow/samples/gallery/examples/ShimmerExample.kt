package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import kotlin.time.Duration

private val Block = Color(0xFFD9DCE3)

/** A profile card whose placeholder blocks shimmer while it is [loading]. */
@Composable
fun ShimmerExample(
    loading: Boolean,
    angle: Float,
    duration: Duration,
    bandWidth: Float,
) {
    Row(
        Modifier
            .width(260.dp)
            .conditionalEffect(
                loading,
                ConditionalEffect.shimmer(angle = angle, duration = duration, bandWidth = bandWidth),
            ).clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3F4F8))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Block))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Block),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Block),
            )
        }
    }
}
