package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** A contact's avatar with particles circling it while the person is [online]. */
@Composable
fun OrbitExample(
    online: Boolean,
    count: Int,
    turn: Duration,
    clockwise: Boolean = true,
    radius: Float = 1.6f,
    trail: Int = 4,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(88.dp)
                .conditionalEffect(
                    online,
                    ConditionalEffect.orbit(
                        count = count,
                        duration = turn,
                        clockwise = clockwise,
                        radius = radius,
                        trail = trail,
                    ),
                ).clip(CircleShape)
                .background(Brush.linearGradient(listOf(KapowEffectColors.violet, KapowEffectColors.cobalt))),
            contentAlignment = Alignment.Center,
        ) {
            KapowText("AM", style = KapowTheme.type.headingSm, color = Color.White)
        }
        KapowCaption(if (online) "Ada is online" else "Ada was here 5 min ago")
    }
}
