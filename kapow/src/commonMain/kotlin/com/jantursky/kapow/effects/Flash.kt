package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

/**
 * The reduced-motion stand-in for effects that move or emit: the element's own pixels briefly lighten and fade
 * back. The runner must draw the element offscreen while [isVisible], so the flash stays on its pixels.
 */
internal class Flash(
    private val color: Color = Color.White,
) {
    private val alpha = Animatable(0f)

    val isVisible: Boolean get() = alpha.value > 0f

    val compositingStrategy: CompositingStrategy
        get() = if (isVisible) CompositingStrategy.Offscreen else CompositingStrategy.Auto

    suspend fun play(
        peak: Float = 0.35f,
        millis: Int = 300,
    ) {
        alpha.snapTo(peak)
        alpha.animateTo(0f, tween(millis))
    }

    fun draw(scope: ContentDrawScope) {
        val a = alpha.value
        if (a > 0f) scope.drawRect(color, alpha = a, blendMode = BlendMode.SrcAtop)
    }
}
