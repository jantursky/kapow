package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.LayoutDirection

/**
 * The outline of [shape] at the element's size, inset so a stroke of the given width stays inside the element, as
 * `Modifier.border` draws it. The path and its length are cached until the size, inset or direction change.
 */
internal class ShapeOutline(
    private val shape: Shape,
) {
    val path = Path()
    private val measure = PathMeasure()
    private var size = Size.Unspecified
    private var inset = -1f
    private var direction: LayoutDirection? = null
    private var density = 0f

    /** The length of [path] around the shape. */
    var length: Float = 0f
        private set

    /** Brings [path] up to date for [scope] with the stroke centered [inset] px inside the edge. */
    fun update(
        scope: DrawScope,
        inset: Float,
    ) {
        if (scope.size == size && inset == this.inset && scope.layoutDirection == direction &&
            scope.density == density
        ) {
            return
        }
        size = scope.size
        this.inset = inset
        direction = scope.layoutDirection
        density = scope.density
        path.reset()
        val inner = Size((size.width - 2 * inset).coerceAtLeast(0f), (size.height - 2 * inset).coerceAtLeast(0f))
        if (inner.minDimension > 0f) {
            path.addOutline(shape.createOutline(inner, scope.layoutDirection, scope))
            path.translate(Offset(inset, inset))
        }
        measure.setPath(path, true)
        length = measure.length
    }

    /** Adds the part of the outline from [from] to [to] px along it to [destination]. */
    fun segment(
        from: Float,
        to: Float,
        destination: Path,
    ) {
        measure.getSegment(from, to, destination, true)
    }
}
