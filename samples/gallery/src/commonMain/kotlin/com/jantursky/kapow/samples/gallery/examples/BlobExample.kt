package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A chat bubble that grows out of a wobbly blob from [origin], such as its tail, and melts back into it. */
@Composable
fun BlobExample(
    visible: Boolean,
    origin: TransformOrigin,
    duration: Duration,
    points: Int = 9,
    wobble: Float = 0.25f,
    seed: Int = 53,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.blob(origin, duration, points, wobble, seed),
        exit = KapowExit.blob(origin, duration, points, wobble, seed),
    ) {
        Column(
            Modifier
                .widthIn(max = 260.dp)
                .background(KapowEffectColors.violet, RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KapowText("On my way. Save me a seat by the window?", color = Color.White)
            KapowCaption("18:42", color = Color.White.copy(alpha = 0.7f))
        }
    }
}
