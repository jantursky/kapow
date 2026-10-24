package com.jantursky.kapow.particles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import com.jantursky.kapow.ParticleContent
import com.jantursky.kapow.ParticleShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Builds the painter for a [ParticleContent], measuring text and sizing paths once per emission. */
internal fun ParticleContent.painter(
    density: Density,
    textMeasurer: () -> TextMeasurer,
): ParticlePainter =
    when (this) {
        is ParticleContent.Shape -> shapePainter(shape, colors, with(density) { size.toPx() })
        is ParticleContent.Image -> {
            val px = with(density) { size.toPx() }
            val filter = tint?.let { ColorFilter.tint(it) }
            ParticlePainter { _, alpha ->
                translate(-px / 2, -px / 2) {
                    with(painter) { draw(Size(px, px), alpha, filter) }
                }
            }
        }
        is ParticleContent.Text -> {
            val layout: TextLayoutResult = textMeasurer().measure(text, style)
            val half = Offset(layout.size.width / 2f, layout.size.height / 2f)
            ParticlePainter { _, alpha -> drawText(layout, topLeft = -half, alpha = alpha) }
        }
    }

internal fun shapePainter(
    shape: ParticleShape,
    colors: List<Color>,
    sizePx: Float,
): ParticlePainter {
    val half = sizePx / 2
    return when (shape) {
        ParticleShape.Circle -> ParticlePainter { i, alpha -> drawCircle(colors[i % colors.size], half, Offset.Zero, alpha) }
        ParticleShape.Square ->
            ParticlePainter { i, alpha ->
                drawRect(colors[i % colors.size], Offset(-half, -half), Size(sizePx, sizePx), alpha)
            }
        else -> {
            val path = unitPath(shape).apply { transform(scaleMatrix(sizePx)) }
            ParticlePainter { i, alpha -> drawPath(path, colors[i % colors.size], alpha) }
        }
    }
}

/** A soft round puff for smoke and dust: [color] in the middle fading to transparent at [radiusPx]. */
internal fun puffPainter(
    color: Color,
    radiusPx: Float,
): ParticlePainter {
    val brush = Brush.radialGradient(listOf(color, color.copy(alpha = color.alpha * 0.6f), Color.Transparent), Offset.Zero, radiusPx)
    return ParticlePainter { _, alpha -> drawCircle(brush, radiusPx, Offset.Zero, alpha) }
}

/** The shape fitted in a 1 × 1 box centered on (0, 0). */
private fun unitPath(shape: ParticleShape): Path =
    Path().apply {
        when (shape) {
            ParticleShape.Star -> {
                for (k in 0 until 10) {
                    val r = if (k % 2 == 0) 0.5f else 0.21f
                    val a = (-90f + k * 36f) * PI.toFloat() / 180f
                    if (k == 0) moveTo(r * cos(a), r * sin(a)) else lineTo(r * cos(a), r * sin(a))
                }
                close()
            }
            ParticleShape.Heart -> {
                moveTo(0f, 0.45f)
                cubicTo(-0.55f, 0.05f, -0.42f, -0.48f, 0f, -0.22f)
                cubicTo(0.42f, -0.48f, 0.55f, 0.05f, 0f, 0.45f)
                close()
            }
            ParticleShape.Sparkle -> {
                val w = 0.09f
                moveTo(0f, -0.5f)
                quadraticTo(w, -w, 0.5f, 0f)
                quadraticTo(w, w, 0f, 0.5f)
                quadraticTo(-w, w, -0.5f, 0f)
                quadraticTo(-w, -w, 0f, -0.5f)
                close()
            }
            ParticleShape.Circle, ParticleShape.Square -> error("drawn without a path")
        }
    }

private fun scaleMatrix(factor: Float) =
    androidx.compose.ui.graphics.Matrix().apply { scale(factor, factor) }
