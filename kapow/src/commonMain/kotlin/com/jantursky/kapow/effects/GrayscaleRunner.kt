package com.jantursky.kapow.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer

/** The saturation left when an [amount] of desaturation is [strength] (0 to 1) of the way in. */
internal fun grayscaleSaturation(
    amount: Float,
    strength: Float,
): Float = 1f - amount.coerceIn(0f, 1f) * strength.coerceIn(0f, 1f)

/**
 * The color matrix that takes [saturation] of the colors, then multiplies the channels by the [tint] pulled
 * [strength] of the way from white to the tint, and by 1 - [dim] * [strength]. Null when there is no tint and no
 * dim: plain desaturation then.
 */
internal fun grayscaleMatrix(
    saturation: Float,
    tint: Color?,
    dim: Float,
    strength: Float,
): FloatArray? {
    if (tint == null && dim <= 0f) return null
    val dimmed = 1f - dim * strength
    val scales =
        floatArrayOf(
            dimmed * (1f + ((tint?.red ?: 1f) - 1f) * strength),
            dimmed * (1f + ((tint?.green ?: 1f) - 1f) * strength),
            dimmed * (1f + ((tint?.blue ?: 1f) - 1f) * strength),
        )
    val inverse = 1f - saturation
    val lum = floatArrayOf(0.213f * inverse, 0.715f * inverse, 0.072f * inverse)
    val m = FloatArray(20)
    for (row in 0 until 3) {
        for (column in 0 until 3) {
            m[row * 5 + column] = (lum[column] + if (row == column) saturation else 0f) * scales[row]
        }
    }
    m[18] = 1f
    return m
}

/**
 * Desaturates the element through a color filter on its layer, and tints and dims it when asked to. Reduced motion
 * switches it without a fade.
 */
internal class GrayscaleRunner(
    private val amount: Float,
    private val tint: Color? = null,
    private val dim: Float = 0f,
) : FadingLoopRunner(loops = false) {
    private var filter: ColorFilter? = null
    private var filtered = 1f

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val saturation = grayscaleSaturation(amount, strength)
        val plain = tint == null && dim <= 0f
        if (if (plain) saturation >= 1f else strength <= 0f) {
            layer.colorFilter = null
            return
        }
        // A new filter only when the amount moved, so a steady gray look allocates nothing per draw.
        val key = if (plain) saturation else strength
        if (key != filtered || filter == null) {
            val matrix = grayscaleMatrix(saturation, tint, dim, strength)
            filter =
                ColorFilter.colorMatrix(
                    if (matrix == null) ColorMatrix().apply { setToSaturation(saturation) } else ColorMatrix(matrix),
                )
            filtered = key
        }
        layer.colorFilter = filter
    }
}
