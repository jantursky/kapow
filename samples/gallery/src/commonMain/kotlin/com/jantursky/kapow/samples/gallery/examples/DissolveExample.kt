package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.CellShape
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

/** A photo that dissolves in square by square, and out again in the reverse order. */
@Composable
fun DissolveExample(
    visible: Boolean,
    cellSize: Dp,
    duration: Duration,
    cellShape: CellShape = CellShape.Square,
    softness: Float = 0f,
    seed: Int = 31,
) {
    KapowAnimatedVisibility(
        visible,
        enter = KapowEnter.dissolve(cellSize, duration, cellShape, softness, seed),
        exit = KapowExit.dissolve(cellSize, duration, cellShape, softness, seed),
    ) {
        Box(Modifier.size(width = 260.dp, height = 170.dp).clip(KapowShapes.card)) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(KapowEffectColors.violet, KapowEffectColors.coral, KapowEffectColors.amber),
                    ),
                )
                val sun = Offset(size.width * 0.68f, size.height * 0.5f)
                drawCircle(Color(0xFFFFE6B0), radius = size.height * 0.16f, center = sun)
                val hills =
                    Path().apply {
                        moveTo(0f, size.height * 0.72f)
                        quadraticTo(size.width * 0.3f, size.height * 0.5f, size.width * 0.6f, size.height * 0.7f)
                        quadraticTo(size.width * 0.85f, size.height * 0.82f, size.width, size.height * 0.62f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                drawPath(hills, Color(0xFF2A1F3D))
            }
            KapowText("Lake Bled, 6:12", Modifier.align(Alignment.BottomStart).padding(14.dp), color = Color.White)
        }
    }
}
