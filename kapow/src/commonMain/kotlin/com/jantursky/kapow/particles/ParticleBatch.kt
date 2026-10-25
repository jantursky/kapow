package com.jantursky.kapow.particles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/** Draws one particle centered on (0, 0). */
internal fun interface ParticlePainter {
    fun DrawScope.drawParticle(
        index: Int,
        alpha: Float,
    )
}

/**
 * One emission in flight: a [ParticleSystem] plus how to draw it. [opacity] and [growth] map a particle's life
 * progress (0 to 1) to its alpha and to a size factor on top of its own scale.
 */
internal class ParticleBatch(
    private val system: ParticleSystem,
    private val painter: ParticlePainter,
    private val opacity: (Float) -> Float = { 1f - it * it * it * it },
    private val growth: (Float) -> Float = { 1f },
) {
    /** Where the emission started, in the coordinates of whoever draws the batch. Set when it is handed over. */
    var origin: Offset = Offset.Zero

    /** A factor on time, for effects played at another speed. */
    var timeScale: Float = 1f

    /** How the batch blends with what is behind it; null draws it normally. */
    var blendMode: BlendMode? = null

    fun step(seconds: Float): Boolean = system.step(seconds * timeScale)

    fun draw(scope: DrawScope) {
        val mode = blendMode
        if (mode == null || mode == BlendMode.SrcOver) {
            drawParticles(scope)
            return
        }
        // The particles are drawn into a layer of their own, which then blends as one.
        val paint = Paint().apply { blendMode = mode }
        scope.drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(-UNBOUNDED, -UNBOUNDED, UNBOUNDED, UNBOUNDED), paint)
            drawParticles(scope)
            canvas.restore()
        }
    }

    private fun drawParticles(scope: DrawScope) {
        with(scope) {
            for (i in 0 until system.count) {
                if (!system.isAlive(i)) continue
                val t = system.progress(i)
                val alpha = opacity(t).coerceIn(0f, 1f)
                val size = system.scale[i] * growth(t)
                if (alpha <= 0f || size <= 0f) continue
                translate(origin.x + system.drawnX(i), origin.y + system.y[i]) {
                    rotate(system.rotation[i], pivot = Offset.Zero) {
                        scale(size, size, pivot = Offset.Zero) {
                            with(painter) { drawParticle(i, alpha) }
                        }
                    }
                }
            }
        }
    }
}

private const val UNBOUNDED = 100_000f
