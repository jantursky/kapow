package com.jantursky.kapow.transitions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.TransitionKind
import kotlin.math.max

/**
 * Breaks the content into triangular shards that crack apart, then fall, turn and fade. Each shard draws the
 * content clipped to its triangle, so the pieces carry whatever the content shows.
 */
internal class ShatterPart(
    private val kind: TransitionKind.Shatter,
) : TransitionPart() {
    private var cachedSize: Size? = null
    private val impactX get() = kind.impact.pivotFractionX
    private var shards: List<Pair<Shard, Path>> = emptyList()

    override fun configureLayer(
        layer: GraphicsLayer,
        progress: Float,
    ) {
        // Fading per drawing rather than through an offscreen buffer, which would clip the pieces to the content.
        layer.compositingStrategy = CompositingStrategy.ModulateAlpha
        layer.alpha = shatterAlpha(1f - progress)
    }

    override fun ContentDrawScope.draw(progress: Float) {
        val t = 1f - progress
        val fall = max(size.height * 1.5f, 160.dp.toPx())
        val gap = 1.5.dp.toPx()
        // The default impact is the middle, which is the content's own center.
        val impact =
            if (kind.impact == TransformOrigin.Center) {
                center
            } else {
                Offset(size.width * kind.impact.pivotFractionX, size.height * kind.impact.pivotFractionY)
            }
        for ((shard, outline) in shards(size)) {
            val offset = shardOffset(shard, t, impact, fall, gap, kind.gravity)
            translate(offset.x, offset.y) {
                rotate(shardRotation(shard, t, kind.spin), pivot = shard.center) {
                    clipPath(outline) { this@draw.drawContent() }
                }
            }
        }
    }

    private fun shards(size: Size): List<Pair<Shard, Path>> {
        if (size != cachedSize) {
            cachedSize = size
            shards =
                shatterShards(size, kind.shards, kind.seed, size.width * impactX).map { shard ->
                    shard to
                        Path().apply {
                            moveTo(shard.xs[0], shard.ys[0])
                            lineTo(shard.xs[1], shard.ys[1])
                            lineTo(shard.xs[2], shard.ys[2])
                            close()
                        }
                }
        }
        return shards
    }
}
