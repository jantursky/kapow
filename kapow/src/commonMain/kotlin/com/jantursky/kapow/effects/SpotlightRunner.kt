package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.platform.blurSupported
import kotlin.math.ceil
import kotlin.math.pow

/**
 * The opacity of each of [layers] equal layers stacked on top of each other so that together they reach [total].
 */
internal fun stackedLayerAlpha(
    total: Float,
    layers: Int,
): Float = 1f - (1f - total.coerceIn(0f, 1f)).pow(1f / layers.coerceAtLeast(1))

/** How far outside the element layer [index] of [layers] keeps clear: from [padding] to [padding] + [softness]. */
internal fun spotlightClearance(
    index: Int,
    layers: Int,
    padding: Float,
    softness: Float,
): Float = if (layers <= 1) padding else padding + softness * index / (layers - 1)

/**
 * Dims everything around the element with a scrim in [color] that leaves the element's [shape] clear, with a soft
 * edge. The scrim is drawn by the element, far past its bounds. Near the element the soft edge is a blurred layer
 * where layers can blur, and a stack of rings with growing holes elsewhere (Android before 12). Reduced motion
 * switches it without a fade.
 */
internal class SpotlightRunner(
    private val color: Color,
    private val shape: Shape,
    private val padding: Dp,
    private val softness: Dp,
    private val canBlur: () -> Boolean = ::blurSupported,
) : FadingLoopRunner(loops = false) {
    private var graphics: GraphicsContext? = null
    private var scrim: GraphicsLayer? = null
    private var hole: GraphicsLayer? = null
    private val rings = Array(RINGS) { Path() }
    private var drawnSize = Size.Unspecified
    private var drawnDensity = 0f
    private var reach = 0f

    override fun attach(context: EffectContext) {
        graphics = context.graphicsContext
    }

    override fun stop() {
        super.stop()
        scrim?.let { graphics?.releaseGraphicsLayer(it) }
        hole?.let { graphics?.releaseGraphicsLayer(it) }
        scrim = null
        hole = null
        drawnSize = Size.Unspecified
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        val s = strength
        if (s <= 0f) return
        val graphics = graphics
        val blur = canBlur() && graphics != null
        val changed = size != drawnSize || density != drawnDensity
        if (changed) {
            drawnSize = size
            drawnDensity = density
            // Whole pixels, so the soft part near the element and the plain rectangles further out meet without a
            // seam. The blur spreads the soft edge a little past the softness.
            reach = ceil(padding.toPx() + softness.toPx() * 1.5f) + 1f
        }
        if (blur) {
            val fresh = scrim == null
            val scrim = scrim ?: graphics.createGraphicsLayer().also { scrim = it }
            val hole = hole ?: graphics.createGraphicsLayer().also { hole = it }
            if (changed || fresh) recordBlurred(scrim, hole)
            // The layer holds the scrim at full dim; the fade is the layer's alpha.
            scrim.alpha = s
            drawLayer(scrim)
        } else {
            if (changed) updateRings()
            val near = color.copy(alpha = stackedLayerAlpha(color.alpha * s, RINGS))
            for (ring in rings) drawPath(ring, near)
        }
        // Further out: four plain rectangles at the full dim, so nothing drawn gets as big as the screen.
        val far = color.copy(alpha = color.alpha * s)
        val w = size.width
        val h = size.height
        drawRect(far, Offset(-FAR, -FAR), Size(w + 2 * FAR, FAR - reach))
        drawRect(far, Offset(-FAR, h + reach), Size(w + 2 * FAR, FAR - reach))
        drawRect(far, Offset(-FAR, -reach), Size(FAR - reach, h + 2 * reach))
        drawRect(far, Offset(w + reach, -reach), Size(FAR - reach, h + 2 * reach))
    }

    /**
     * The scrim near the element: a plain rectangle up to [reach], with the element's shape cut out of it by a
     * blurred copy of the shape. Only the cut is blurred, so the rectangle's own edges stay sharp and meet the
     * rectangles further out.
     */
    private fun DrawScope.recordBlurred(
        scrim: GraphicsLayer,
        hole: GraphicsLayer,
    ) {
        val soft = softness.toPx()
        val r = reach.toInt()
        val base = size
        val box = IntSize(base.width.toInt() + 2 * r, base.height.toInt() + 2 * r)
        val grow = padding.toPx() + soft * 0.6f
        val outline = shape.createOutline(Size(base.width + 2 * grow, base.height + 2 * grow), layoutDirection, this)
        hole.renderEffect = if (soft > 0f) BlurEffect(soft / 2, soft / 2, TileMode.Decal) else null
        hole.blendMode = BlendMode.DstOut
        hole.record(this, layoutDirection, box) {
            translate(reach - grow, reach - grow) { drawOutline(outline, Color.Black) }
        }
        scrim.topLeft = IntOffset(-r, -r)
        scrim.compositingStrategy = CompositingStrategy.Offscreen
        scrim.record(this, layoutDirection, box) {
            drawRect(color)
            drawLayer(hole)
        }
    }

    private fun DrawScope.updateRings() {
        val pad = padding.toPx()
        val soft = softness.toPx()
        val box = Rect(-reach, -reach, size.width + reach, size.height + reach)
        rings.forEachIndexed { i, ring ->
            val grow = spotlightClearance(i, RINGS, pad, soft)
            ring.reset()
            ring.fillType = PathFillType.EvenOdd
            ring.addRect(box)
            val hole = Size(size.width + 2 * grow, size.height + 2 * grow)
            val outline = Path().apply { addOutline(shape.createOutline(hole, layoutDirection, this@updateRings)) }
            outline.translate(Offset(-grow, -grow))
            ring.addPath(outline)
        }
    }

    private companion object {
        const val RINGS = 16

        // How far the scrim reaches past the element on every side, in pixels: past any screen.
        const val FAR = 20_000f
    }
}
