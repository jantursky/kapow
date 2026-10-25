package com.jantursky.kapow.transitions

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.TransitionKind
import kotlin.math.floor
import kotlin.math.hypot

/**
 * The torn edge of a wipe at [progress], as x, y pairs in a frame centered on the content where the edge runs
 * top to bottom and moves right. The edge's middle travels from -([reach] + [tooth]) to [reach] + [tooth], so the
 * tear, up to a [tooth] off the middle on either side, clears the content at both ends. It wanders slowly over a
 * few teeth and is jagged in between, like torn paper, and the same [reach] and [tooth] always give the same edge.
 */
internal fun tearEdge(
    reach: Float,
    tooth: Float,
    progress: Float,
    roughness: Float = TEAR_ROUGHNESS,
): FloatArray {
    val depth = tooth
    val middle = wipeEdge(reach + depth, progress)
    val points = ArrayList<Float>()
    // The share of the edge that is jagged; the rest wanders slowly. The default keeps the 0.6 / 0.4 split.
    val calm = if (roughness == TEAR_ROUGHNESS) 0.6f else 1f - roughness
    var y = -reach - tooth
    var i = 0
    while (true) {
        val jag = if (i % 2 == 0) 1f else -1f
        val offset = calm * tearWander((y + reach) / (tooth * 4)) + roughness * jag * cellHash(i, 1, TEAR_SEED)
        points += middle + depth * offset
        points += y
        if (y > reach) break
        y += tooth * (0.2f + 0.4f * cellHash(i, 0, TEAR_SEED))
        i++
    }
    return points.toFloatArray()
}

/** Smooth noise from -1 to 1 along [t]: fixed random values at whole numbers, eased in between. */
internal fun tearWander(t: Float): Float {
    val k = floor(t).toInt()
    val f = t - k
    val eased = f * f * (3 - 2 * f)
    val a = cellHash(k, 2, TEAR_SEED)
    val b = cellHash(k + 1, 2, TEAR_SEED)
    return 2 * (a + (b - a) * eased) - 1
}

private const val TEAR_SEED = 47

/** How much of the tear is jagged by default, from 0 (a slow wander) to 1 (all jags). */
internal const val TEAR_ROUGHNESS = 0.4f

/** A wipe at an angle, like [cutWipe], whose leading edge is a tear with a light rim of paper along it. */
internal class TearMask(
    private val kind: TransitionKind.Tear,
) : TransitionPart() {
    override fun ContentDrawScope.draw(progress: Float) {
        drawContent()
        val reach = hypot(size.width, size.height) / 2
        val edge = tearEdge(reach, kind.toothSize.toPx(), progress, kind.roughness)
        val line = Path()
        for (i in edge.indices step 2) {
            if (i == 0) line.moveTo(edge[i], edge[i + 1]) else line.lineTo(edge[i], edge[i + 1])
        }
        // Everything right of the edge is still hidden.
        val hidden =
            Path().apply {
                addPath(line)
                lineTo(reach * 3, edge[edge.size - 1])
                lineTo(reach * 3, edge[1])
                close()
            }
        rotate(kind.angle, pivot = center) {
            translate(center.x, center.y) {
                drawPath(hidden, Color.Black, blendMode = BlendMode.DstOut)
                if (kind.rimColor.alpha > 0f) {
                    drawPath(
                        line,
                        kind.rimColor,
                        style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Miter),
                        blendMode = BlendMode.SrcAtop,
                    )
                }
            }
        }
    }
}
