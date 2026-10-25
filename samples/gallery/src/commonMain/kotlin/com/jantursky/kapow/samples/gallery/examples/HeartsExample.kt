package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A like button whose heart pops, with small hearts floating up, each time [likes] changes. */
@Composable
fun HeartsExample(
    likes: Int,
    color: Color,
    count: Int,
    bigHeartScale: Float = 1f,
) {
    val colors = KapowTheme.colors
    Row(
        Modifier
            .clip(KapowShapes.control)
            .background(colors.panel)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(
            Modifier
                .size(32.dp)
                .changeEffect(
                    likes,
                    ChangeEffect.hearts(color, count, bigHeartScale = bigHeartScale),
                    isEnabled = likes > 0,
                ),
        ) {
            val w = size.width
            val h = size.height
            val heart =
                Path().apply {
                    moveTo(w * 0.5f, h * 0.88f)
                    cubicTo(w * -0.1f, h * 0.5f, w * 0.1f, h * 0.05f, w * 0.5f, h * 0.3f)
                    cubicTo(w * 0.9f, h * 0.05f, w * 1.1f, h * 0.5f, w * 0.5f, h * 0.88f)
                    close()
                }
            drawPath(heart, if (likes > 0) color else colors.slate)
        }
        KapowText("$likes likes", color = colors.ink)
    }
}
