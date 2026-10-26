package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A broadcast button that sends out rings while it is [live]. The rings follow the button's [shape]. */
@Composable
fun PingExample(
    live: Boolean,
    color: Color,
    shape: Shape,
    rings: Int,
    scale: Float,
    duration: Duration,
) {
    Box(
        Modifier
            .size(96.dp)
            .conditionalEffect(live, ConditionalEffect.ping(color, shape, rings, scale, duration))
            .background(Color(0xFFE5484D), shape),
        contentAlignment = Alignment.Center,
    ) {
        KapowText(if (live) "LIVE" else "OFF", color = Color.White)
    }
}
