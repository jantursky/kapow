package com.jantursky.kapow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A star with [points] tips and one tip straight up, stretched to the size it is given. Its inner corners sit at
 * [innerRatio] of the tips' distance from the center: 0.5 is a classic star, lower values a spiky comic burst.
 * Made for [KapowEnter.shapeIris], and usable anywhere a [Shape] is.
 */
@Immutable
public class StarShape(
    public val points: Int = 5,
    public val innerRatio: Float = 0.5f,
) : Shape {
    init {
        require(points >= 3) { "points must be at least 3" }
        require(innerRatio > 0f && innerRatio <= 1f) { "innerRatio must be in (0, 1]" }
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val rx = size.width / 2
        val ry = size.height / 2
        for (k in 0 until 2 * points) {
            val r = if (k % 2 == 0) 1f else innerRatio
            val a = (-90f + k * 180f / points) * PI.toFloat() / 180f
            val x = rx + rx * r * cos(a)
            val y = ry + ry * r * sin(a)
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is StarShape && points == other.points && innerRatio == other.innerRatio

    override fun hashCode(): Int = 31 * points + innerRatio.hashCode()

    override fun toString(): String = "StarShape(points=$points, innerRatio=$innerRatio)"
}
