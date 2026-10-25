package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.FillDirection
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
    waves: Float = WAVES,
    roll: Float = ROLL,
): Float {
    val p = progress.coerceIn(0f, 1f)
    val level = height * (1f - p)
    val a = fillAmplitude(p, amplitude)
    val angle = 2 * PI.toFloat() * (x / width * waves + p * roll) + phase
    return level + a * sin(angle)
}

private const val WAVE_EASE = 0.08f

// Waves across the element, and how many wave lengths the surface rolls by over the whole progress.
private const val WAVES = 1.5f
private const val ROLL = 3f

/** The turn, in degrees, that makes a liquid filling upward fill in [direction] instead. */
internal fun fillRotation(direction: FillDirection): Float =
    when (direction) {
        FillDirection.Up -> 0f
        FillDirection.Down -> 180f
        FillDirection.Right -> 90f
        FillDirection.Left -> -90f
    }

/**
 * A tinted liquid that fills the element from one side up to the progress, with a wave on its surface. It rises
 * in [direction], has [waves] waves across and rolls them [roll] wave lengths over the whole progress. The
 * liquid's color is [color], or follows [colorRamp] with the progress. A [foam] wide line in [foamColor] rides
 * its surface.
 */
internal class FillProgressRunner(
    private val color: Color,
    private val wave: Dp,
    private val direction: FillDirection = FillDirection.Up,
    private val waves: Float = WAVES,
    private val roll: Float = ROLL,
    private val colorRamp: List<Color>? = null,
    private val foam: Dp = 0.dp,
    private val foamColor: Color = Color.White,
) : ProgressRunner() {
    private val front = Path()
    private val back = Path()
    private val surface = Path()

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy =
            if (progress() > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val p = progress().coerceIn(0f, 1f)
        if (p <= 0f) return
        if (direction == FillDirection.Up) {
            drawLiquid(p, size.width, size.height)
            return
        }
        // Every other direction is the upward fill turned around the center, on a stage of the turned size.
        val turned = direction == FillDirection.Left || direction == FillDirection.Right
        val w = if (turned) size.height else size.width
        val h = if (turned) size.width else size.height
        rotate(fillRotation(direction), pivot = center) {
            translate(center.x - w / 2, center.y - h / 2) { drawLiquid(p, w, h) }
        }
    }

    private fun DrawScope.drawLiquid(
        p: Float,
        w: Float,
        h: Float,
    ) {
        val amplitude = wave.toPx()
        val tint = if (colorRamp != null) rampColor(colorRamp, p) else color
        // A paler wave behind, out of step with the front one, gives the surface some depth.
        liquid(back, p, amplitude, PI.toFloat(), w, h)
        drawPath(back, tint, alpha = 0.5f, blendMode = BlendMode.SrcAtop)
        liquid(front, p, amplitude, 0f, w, h, surface)
        drawPath(front, tint, blendMode = BlendMode.SrcAtop)
        if (foam > 0.dp) {
            drawPath(
                surface,
                foamColor,
                alpha = FOAM_ALPHA,
                style = Stroke(foam.toPx(), cap = StrokeCap.Round),
                blendMode = BlendMode.SrcAtop,
            )
        }
    }

    private fun liquid(
        path: Path,
        progress: Float,
        amplitude: Float,
        phase: Float,
        w: Float,
        h: Float,
        line: Path? = null,
    ) {
        val steps = ceil(w / STEP_PX).toInt().coerceAtLeast(1)
        path.rewind()
        line?.rewind()
        path.moveTo(0f, h)
        for (i in 0..steps) {
            val x = w * i / steps
            val y = fillSurface(x, w, h, progress, amplitude, phase, waves, roll)
            path.lineTo(x, y)
            if (line != null) {
                if (i == 0) {
                    line.moveTo(x, y)
                } else {
                    line.lineTo(x, y)
                }
            }
        }
        path.lineTo(w, h)
        path.close()
    }

    private companion object {
        const val STEP_PX = 6f
        const val FOAM_ALPHA = 0.85f
    }
}
