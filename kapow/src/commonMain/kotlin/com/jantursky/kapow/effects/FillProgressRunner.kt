package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Dp
import com.jantursky.kapow.internal.ProgressRunner
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

/**
 * How tall the surface wave is at [progress], at most [amplitude]: flat at the bottom and at the top, so the fill
 * starts from nothing and ends covering the whole element.
 */
internal fun fillAmplitude(
    progress: Float,
    amplitude: Float,
): Float {
    val p = progress.coerceIn(0f, 1f)
    return amplitude * minOf(1f, p / WAVE_EASE, (1f - p) / WAVE_EASE)
}

/**
 * The height of the liquid's surface above [x] in an element [width] by [height] px at [progress]: the level rises
 * from the bottom to the top, and the wave rolls sideways as the progress changes. [phase] shifts the wave.
 */
internal fun fillSurface(
    x: Float,
    width: Float,
    height: Float,
    progress: Float,
    amplitude: Float,
    phase: Float = 0f,
): Float {
    val p = progress.coerceIn(0f, 1f)
    val level = height * (1f - p)
    val a = fillAmplitude(p, amplitude)
    val angle = 2 * PI.toFloat() * (x / width * WAVES + p * ROLL) + phase
    return level + a * sin(angle)
}

private const val WAVE_EASE = 0.08f

// Waves across the element, and how many wave lengths the surface rolls by over the whole progress.
private const val WAVES = 1.5f
private const val ROLL = 3f

/** A tinted liquid that fills the element from the bottom up to the progress, with a wave on its surface. */
internal class FillProgressRunner(
    private val color: Color,
    private val wave: Dp,
) : ProgressRunner() {
    private val front = Path()
    private val back = Path()

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy =
            if (progress() > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val p = progress().coerceIn(0f, 1f)
        if (p <= 0f) return
        val amplitude = wave.toPx()
        // A paler wave behind, out of step with the front one, gives the surface some depth.
        liquid(back, p, amplitude, PI.toFloat())
        drawPath(back, color, alpha = 0.5f, blendMode = BlendMode.SrcAtop)
        liquid(front, p, amplitude, 0f)
        drawPath(front, color, blendMode = BlendMode.SrcAtop)
    }

    private fun DrawScope.liquid(
        path: Path,
        progress: Float,
        amplitude: Float,
        phase: Float,
    ) {
        val w = size.width
        val h = size.height
        val steps = ceil(w / STEP_PX).toInt().coerceAtLeast(1)
        path.rewind()
        path.moveTo(0f, h)
        for (i in 0..steps) {
            val x = w * i / steps
            path.lineTo(x, fillSurface(x, w, h, progress, amplitude, phase))
        }
        path.lineTo(w, h)
        path.close()
    }

    private companion object {
        const val STEP_PX = 6f
    }
}
