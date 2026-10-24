package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.samples.design.KapowEffectColors

/** A photo in an editor, outlined with marching ants while it is [selected]. */
@Composable
fun MarchingAntsExample(
    selected: Boolean,
    dash: Dp,
    speed: Dp,
) {
    val shape = RoundedCornerShape(12.dp)
    Canvas(
        Modifier
            .size(width = 220.dp, height = 150.dp)
            .conditionalEffect(selected, ConditionalEffect.marchingAnts(shape = shape, dash = dash, speed = speed))
            .clip(shape),
    ) {
        // A sunset over two hills, standing in for the picture.
        drawRect(
            Brush.verticalGradient(listOf(KapowEffectColors.violet, KapowEffectColors.coral, KapowEffectColors.amber)),
        )
        drawCircle(Color(0xFFFFF1C2), size.minDimension * 0.16f, Offset(size.width * 0.68f, size.height * 0.52f))
        val far =
            Path().apply {
                moveTo(0f, size.height * 0.7f)
                quadraticTo(size.width * 0.3f, size.height * 0.45f, size.width * 0.62f, size.height * 0.72f)
                lineTo(size.width, size.height * 0.62f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(far, Color(0xFF2B2466))
        val near =
            Path().apply {
                moveTo(0f, size.height * 0.86f)
                quadraticTo(size.width * 0.55f, size.height * 0.66f, size.width, size.height * 0.9f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(near, Color(0xFF161233))
    }
}
