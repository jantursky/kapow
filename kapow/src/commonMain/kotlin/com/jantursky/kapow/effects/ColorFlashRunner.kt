package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * How strongly the flash covers the element at [progress], from 0 to 1: it snaps up over the first tenth, then
 * eases back out, fast at first and gently at the end.
 */
internal fun flashStrength(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    if (p < FLASH_ATTACK) return p / FLASH_ATTACK
    val rest = 1f - (p - FLASH_ATTACK) / (1f - FLASH_ATTACK)
    return rest * rest
}

private const val FLASH_ATTACK = 0.1f

/** Tints the element's own pixels in a color that fades back. Transparent parts of the element stay clear. */
internal class ColorFlashRunner(
    private val color: Color,
    private val duration: Duration,
    private val fullPeak: Float = FULL_PEAK,
) : ChangeRunner() {
    private val progress = Animatable(1f)
    private var peak = fullPeak
    private var job: Job? = null

    private val strength: Float get() = if (progress.value < 1f) flashStrength(progress.value) * peak else 0f

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        // SrcAtop needs the element's pixels in a buffer of their own, but only while the flash shows.
        layer.compositingStrategy = if (strength > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val a = strength
        if (a > 0f) drawRect(color, alpha = a, blendMode = BlendMode.SrcAtop)
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        peak = if (context.isReducedMotion()) minOf(REDUCED_PEAK, fullPeak) else fullPeak
        val millis = duration.at(context.speed).inWholeMilliseconds.toInt()
        job =
            context.scope.launch {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(millis, easing = LinearEasing))
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        const val FULL_PEAK = 0.85f
        const val REDUCED_PEAK = 0.35f
    }
}
