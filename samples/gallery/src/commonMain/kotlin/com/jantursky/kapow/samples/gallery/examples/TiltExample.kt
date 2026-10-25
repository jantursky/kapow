package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.kapowTilt
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText

private val Ink = Color(0xFF161616)

/**
 * A card that tilts toward the pointer. The tap counter inside is a plain clickable: the tilt only watches the
 * pointer, so the taps still arrive.
 */
@Composable
fun TiltExample(
    taps: Int,
    onTap: () -> Unit,
    color: Color,
    maxAngle: Float,
    sheen: Boolean,
    enabled: Boolean,
) {
    Column(
        Modifier
            .width(240.dp)
            .height(150.dp)
            .kapowTilt(
                maxAngle = maxAngle,
                sheen = if (sheen) Color.White.copy(alpha = 0.4f) else null,
                enabled = enabled,
            ).clip(KapowShapes.control)
            .background(color)
            .clickable(onClick = onTap)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        KapowText("Hover or press and drag", color = Ink)
        KapowText("Tapped $taps times", color = Ink)
    }
}
