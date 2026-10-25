package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.internal.ProgressRunner
import com.jantursky.kapow.transitions.cutOutside
import com.jantursky.kapow.transitions.farthestCorner
import com.jantursky.kapow.transitions.irisPath
import com.jantursky.kapow.transitions.originPoint

/**
 * The radius of an iris with a soft edge [soft] px wide at [progress], where the farthest corner is [reach] px
 * away. The solid part reaches [reach] at progress 1 and the soft part is outside it, so the element is fully
 * shown (or hidden, inverted) at the ends.
 */
internal fun softIrisRadius(
    reach: Float,
    soft: Float,
    progress: Float,
): Float = (reach + soft) * progress.coerceIn(0f, 1f)

/** Shows the element inside a circle around [origin] whose radius follows the progress. */
internal class IrisProgressRunner(
    private val origin: TransformOrigin,
    private val softEdge: Dp = 0.dp,
    private val shape: Shape? = null,
    private val invert: Boolean = false,
) : ProgressRunner() {
    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val p = progress()
        layer.compositingStrategy = if (p > 0f && p < 1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }

    override fun ContentDrawScope.drawEffect() {
        val p = progress().coerceIn(0f, 1f)
        // Revealing shows nothing at 0 and everything at 1; inverted, it is the other way round.
        if (if (invert) p >= 1f else p <= 0f) return
        drawContent()
        if (if (invert) p <= 0f else p >= 1f) return
        val soft = softEdge.toPx()
        if (soft <= 0f && shape == null && !invert) {
            cutOutside(irisPath(size, origin, p))
            return
        }
        val point = originPoint(size, origin)
        val radius = softIrisRadius(farthestCorner(size, point), soft, p)
        if (soft > 0f && shape == null) {
            val solid = ((radius - soft) / radius).coerceIn(0f, 1f)
            val fade =
                Brush.radialGradient(
                    0f to Color.Black,
                    solid to Color.Black,
                    1f to Color.Transparent,
                    center = point,
                    radius = radius,
                )
            val mode = if (invert) BlendMode.DstOut else BlendMode.DstIn
            drawRect(fade, Offset(-1f, -1f), Size(size.width + 2f, size.height + 2f), blendMode = mode)
            return
        }
        val hole =
            if (shape == null) {
                irisPath(size, origin, p)
            } else {
                Path().apply {
                    addOutline(shape.createOutline(Size(2 * radius, 2 * radius), layoutDirection, this@drawEffect))
                    translate(Offset(point.x - radius, point.y - radius))
                }
            }
        if (invert) {
            drawPath(hole, Color.Black, blendMode = BlendMode.DstOut)
        } else {
            cutOutside(hole)
        }
    }
}
