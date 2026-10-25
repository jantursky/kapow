package com.jantursky.kapow.effects

import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.ProgressRunner
import kotlin.math.min

/** The color [colors] reach at [progress] (0 to 1): the first at 0, the last at 1, blended in between. */
internal fun rampColor(
    colors: List<Color>,
    progress: Float,
): Color {
    if (colors.size == 1) return colors[0]
    val p = progress.coerceIn(0f, 1f) * (colors.size - 1)
    val i = p.toInt().coerceAtMost(colors.size - 2)
    return lerp(colors[i], colors[i + 1], p - i)
}

/**
 * A halo whose strength follows the progress, and the element brightened by a quarter of it. With [alphaCurve] the
 * halo's opacity is scaled by the curve at the progress, and with [colorRamp] its color follows the ramp. Either
 * one draws the halo in one of [STEPS] shades, so it never builds more than that many layers.
 */
internal class GlowProgressRunner(
    color: Color,
    private val radius: Dp,
    private val shape: Shape,
    private val alphaCurve: Easing? = null,
    private val colorRamp: List<Color>? = null,
) : ProgressRunner() {
    private val plain = Halo(color, radius, shape)
    private val shades = arrayOfNulls<Halo>(STEPS)
    private val base = color
    private var graphics: GraphicsContext? = null
    private var content: GraphicsLayer? = null

    private fun haloAt(progress: Float): Halo {
        if (alphaCurve == null && colorRamp == null) return plain
        val step = (progress * (STEPS - 1)).toInt().coerceIn(0, STEPS - 1)
        shades[step]?.let { return it }
        val at = step / (STEPS - 1f)
        val tint = if (colorRamp != null) rampColor(colorRamp, at) else base
        val factor = alphaCurve?.transform(at) ?: 1f
        return Halo(tint.copy(alpha = tint.alpha * factor.coerceIn(0f, 1f)), radius, shape).also {
            graphics?.let { context -> it.attach(context) }
            shades[step] = it
        }
    }

    override fun ContentDrawScope.drawEffect() {
        val p = progress().coerceIn(0f, 1f)
        val amount = p * MAX_HALO
        haloAt(p).draw(this, amount)
        val brighten = min(amount, MAX_HALO) / MAX_HALO * 0.25f
        val graphics = graphics
        if (brighten <= 0f || graphics == null) {
            drawContent()
            return
        }
        // Brightening only the element's own pixels needs them in a buffer of their own. It is a separate layer so
        // the halo, which reaches outside the element, isn't clipped to it.
        val layer =
            content ?: graphics.createGraphicsLayer().also {
                it.compositingStrategy = CompositingStrategy.Offscreen
                content = it
            }
        layer.record {
            this@drawEffect.drawContent()
            drawRect(Color.White, alpha = brighten, blendMode = BlendMode.SrcAtop)
        }
        drawLayer(layer)
    }

    override fun attach(context: EffectContext) {
        graphics = context.graphicsContext
        plain.attach(context.graphicsContext)
        for (halo in shades) halo?.attach(context.graphicsContext)
    }

    override fun stop() {
        plain.release()
        for (i in shades.indices) {
            shades[i]?.release()
            shades[i] = null
        }
        content?.let { graphics?.releaseGraphicsLayer(it) }
        content = null
    }

    private companion object {
        const val STEPS = 24
    }
}
