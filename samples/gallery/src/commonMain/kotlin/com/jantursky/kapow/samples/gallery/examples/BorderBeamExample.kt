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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowNumeric
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** A voice memo card with a beam running around it while [recording] is true. */
@Composable
fun BorderBeamExample(
    recording: Boolean,
    color: Color,
    width: Dp,
    lap: Duration,
    beams: Int = 1,
    clockwise: Boolean = true,
    tailFade: Float = 1.5f,
    trackAlpha: Float = 0.18f,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .width(240.dp)
            .conditionalEffect(
                recording,
                ConditionalEffect.borderBeam(
                    color,
                    width,
                    shape,
                    duration = lap,
                    trackAlpha = trackAlpha,
                    beams = beams,
                    clockwise = clockwise,
                    tailFade = tailFade,
                ),
            ).clip(shape)
            .background(KapowTheme.colors.panel)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(if (recording) color else KapowTheme.colors.hairline))
        Column(Modifier.weight(1f)) {
            KapowText(if (recording) "Recording" else "Paused", style = KapowTheme.type.subheading)
            KapowCaption("Voice memo")
        }
        KapowNumeric("00:42")
    }
}
