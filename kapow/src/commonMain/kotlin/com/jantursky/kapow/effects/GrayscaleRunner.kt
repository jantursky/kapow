package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer

/** The saturation left when an [amount] of desaturation is [strength] (0 to 1) of the way in. */
internal fun grayscaleSaturation(
    amount: Float,
    strength: Float,
): Float = 1f - amount.coerceIn(0f, 1f) * strength.coerceIn(0f, 1f)

/** Desaturates the element through a color filter on its layer. Reduced motion switches it without a fade. */
internal class GrayscaleRunner(
    private val amount: Float,
) : FadingLoopRunner(loops = false) {
    private var filter: ColorFilter? = null
    private var filtered = 1f

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val saturation = grayscaleSaturation(amount, strength)
        if (saturation >= 1f) {
            layer.colorFilter = null
            return
        }
        // A new filter only when the amount moved, so a steady gray look allocates nothing per draw.
        if (saturation != filtered || filter == null) {
            filter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) })
            filtered = saturation
        }
        layer.colorFilter = filter
    }
}
