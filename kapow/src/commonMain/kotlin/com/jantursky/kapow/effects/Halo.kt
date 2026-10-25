// The halo strength cap of 1.5 and its square-root opacity are derived from Pow
// (https://github.com/EmergeTools/Pow), MIT, see NOTICE.
package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.jantursky.kapow.platform.blurSupported
import kotlin.math.ceil
import kotlin.math.sqrt

/** The most a halo builds up to, however often it is triggered. */
internal const val MAX_HALO = 1.5f

/** How opaque a halo of [amount] (0 to [MAX_HALO]) is at its core. */
internal fun haloOpacity(amount: Float): Float = (sqrt(amount.coerceIn(0f, MAX_HALO)) / 1.2f).coerceAtMost(1f)

/**
 * A soft glow in [shape] around an element. Where layers can blur it draws the shape once, blurred; elsewhere
 * (Android before 12) it stacks a few grown copies of the shape at falling opacity.
 */
internal class Halo(
    private val color: Color,
    private val radius: Dp,
    private val shape: Shape,
    private val limit: Float = MAX_HALO,
    private val canBlur: () -> Boolean = ::blurSupported,
) {
    private var graphics: GraphicsContext? = null
    private var layer: GraphicsLayer? = null

    fun attach(graphics: GraphicsContext) {
        this.graphics = graphics
    }

    fun release() {
        layer?.let { graphics?.releaseGraphicsLayer(it) }
        layer = null
    }

    fun draw(
        scope: DrawScope,
        amount: Float,
    ) = with(scope) {
        val a = amount.coerceIn(0f, limit)
        if (a <= 0f) return@with
        val reach = radius.toPx() * a
        val alpha = haloOpacity(a)
        val graphics = graphics
        if (canBlur() && graphics != null) {
            val layer = layer ?: graphics.createGraphicsLayer().also { layer = it }
            val pad = ceil(reach * 2).toInt()
            val spread = reach / 2
            layer.renderEffect = BlurEffect(reach / 2, reach / 2, TileMode.Decal)
            layer.topLeft = IntOffset(-pad, -pad)
            val base = size
            layer.record(this, layoutDirection, IntSize(base.width.toInt() + 2 * pad, base.height.toInt() + 2 * pad)) {
                translate(pad - spread, pad - spread) { drawGrown(base, spread, alpha) }
            }
            drawLayer(layer)
        } else {
            for (step in 1..STACK_STEPS) {
                val grow = reach * step / STACK_STEPS
                translate(-grow, -grow) { drawGrown(size, grow, alpha * (1f - step / (STACK_STEPS + 1f)) / 2) }
            }
        }
    }

    private fun DrawScope.drawGrown(
        base: Size,
        grow: Float,
        alpha: Float,
    ) {
        val grown = Size(base.width + 2 * grow, base.height + 2 * grow)
        drawOutline(shape.createOutline(grown, layoutDirection, this), color, alpha)
    }

    private companion object {
        const val STACK_STEPS = 5
    }
}
