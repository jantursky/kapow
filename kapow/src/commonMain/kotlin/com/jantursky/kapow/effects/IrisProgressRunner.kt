package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.jantursky.kapow.internal.ProgressRunner
import com.jantursky.kapow.transitions.cutOutside
import com.jantursky.kapow.transitions.irisPath

/** Shows the element inside a circle around [origin] whose radius follows the progress. */
internal class IrisProgressRunner(
    private val origin: TransformOrigin,
) : ProgressRunner() {
    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val p = progress()
        layer.compositingStrategy = if (p > 0f && p < 1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }

    override fun ContentDrawScope.drawEffect() {
        val p = progress().coerceIn(0f, 1f)
        if (p <= 0f) return
        drawContent()
        if (p < 1f) cutOutside(irisPath(size, origin, p))
    }
}
