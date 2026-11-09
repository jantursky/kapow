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
import kotlin.math.ceil
import kotlin.math.floor

/** How far a painter without its own [ParticlePainter.extent] reaches from the particle's center, in pixels. */
internal const val DEFAULT_PAINTER_EXTENT = 96f

/** Draws one particle centered on (0, 0). */
internal fun interface ParticlePainter {
    /**
     * How far from (0, 0) the painter can draw at scale 1, in pixels. A blended batch limits its layer to the
     * particles' bounds grown by this times their size, so it must not be smaller than what the painter draws.
     */
    val extent: Float get() = DEFAULT_PAINTER_EXTENT

    fun DrawScope.drawParticle(
        index: Int,
        alpha: Float,
    )
}

/** A painter that draws with [draw] and never reaches further than [extent] pixels from the particle's center. */
internal fun painterOf(
    extent: Float,
    draw: DrawScope.(index: Int, alpha: Float) -> Unit,
): ParticlePainter =
    object : ParticlePainter {
        override val extent: Float = extent

        override fun DrawScope.drawParticle(
            index: Int,
            alpha: Float,
        ) = draw(index, alpha)
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

    /**
     * Where the origin is now, asked for on each draw by a batch a particle layer holds, so particles follow an
     * emitter that moves or scrolls. Null keeps the last origin, which is what a batch does once it is [freeze]n.
     */
    var follow: (() -> Offset?)? = null

    /** True once the last particle is gone. */
    var finished: Boolean = false
        private set

    /** Moves the origin to where the emitter is now, if it is followed and still there. */
    internal fun followEmitter() {
        follow?.invoke()?.let { origin = it }
    }

    /** Stops following the emitter; the particles carry on from where the origin is now. */
    fun freeze() {
        follow = null
    }

    /** A factor on time, for effects played at another speed. */
    var timeScale: Float = 1f

    /** How the batch blends with what is behind it; null draws it normally. */
    var blendMode: BlendMode? = null

    // One paint for the batch, made when the first blended frame is drawn and kept while the mode stays the same.
    private var blendPaint: Paint? = null

    fun step(seconds: Float): Boolean = system.step(seconds * timeScale).also { if (!it) finished = true }

    fun draw(scope: DrawScope) {
        followEmitter()
        val mode = blendMode
        if (mode == null || mode == BlendMode.SrcOver) {
            drawParticles(scope)
            return
        }
        // The particles are drawn into a layer of their own, which then blends as one. The layer covers only
        // where they are.
        val bounds = liveBounds() ?: return
        val paint = paintFor(mode)
        scope.drawIntoCanvas { canvas ->
            canvas.saveLayer(bounds, paint)
            drawParticles(scope)
            canvas.restore()
        }
    }

    private fun paintFor(mode: BlendMode): Paint {
        val kept = blendPaint
        if (kept != null && kept.blendMode == mode) return kept
        paintCreations++
        return Paint().apply { blendMode = mode }.also { blendPaint = it }
    }

    /** The area the live particles can draw in, or null when none is drawn. */
    internal fun liveBounds(): Rect? {
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        val reach = painter.extent
        for (i in 0 until system.count) {
            if (!system.isAlive(i)) continue
            val t = system.progress(i)
            val size = system.scale[i] * growth(t)
            if (opacity(t).coerceIn(0f, 1f) <= 0f || size <= 0f) continue
            val x = origin.x + system.drawnX(i)
            val y = origin.y + system.y[i]
            val r = reach * size + LAYER_MARGIN
            left = minOf(left, x - r)
            top = minOf(top, y - r)
            right = maxOf(right, x + r)
            bottom = maxOf(bottom, y + r)
        }
        return if (left > right) null else Rect(floor(left), floor(top), ceil(right), ceil(bottom))
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

/** Pixels added around the particles' bounds, so anti-aliased edges are not cut. */
private const val LAYER_MARGIN = 2f

/** How many paints blended batches have made, for tests. */
internal var paintCreations: Int = 0
