package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.ShakeAxis
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A password field that shakes each time [attempts] changes, as it does for a wrong password. */
@Composable
fun ShakeExample(
    attempts: Int,
    axis: ShakeAxis,
    distance: Dp,
    swings: Int,
    rotation: Float,
    duration: Duration,
) {
    Box(
        Modifier
            .size(width = 200.dp, height = 56.dp)
            .changeEffect(attempts, ChangeEffect.shake(axis, distance, swings, rotation, duration))
            .clip(KapowShapes.card)
            .background(KapowEffectColors.coral),
        contentAlignment = Alignment.Center,
    ) {
        KapowText("Wrong password", color = Color.White)
    }
}
