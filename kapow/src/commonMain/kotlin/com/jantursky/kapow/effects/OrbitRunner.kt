package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import com.jantursky.kapow.ParticleContent
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.particles.ParticlePainter
import com.jantursky.kapow.particles.painter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration

/**
 * Where a particle at [angle] radians is on an orbit with radii [radiusX] and [radiusY] around (0, 0), the
 * ellipse tipped by [tilt] radians. Angle 0 is the right end; the far half of the orbit is the top one.
 */
internal fun orbitPosition(
    angle: Float,
    radiusX: Float,
    radiusY: Float,
    tilt: Float,
): Offset {
    val x = radiusX * cos(angle)
    val y = radiusY * sin(angle)
    return Offset(x * cos(tilt) - y * sin(tilt), x * sin(tilt) + y * cos(tilt))
}

/** How near a particle at [angle] is, from -1 (behind the element) to 1 (in front of it). */
internal fun orbitDepth(angle: Float): Float = sin(angle)

/** A particle's size factor at [depth]: [behind] of the full size at the back, full size in front. */
internal fun orbitScale(
    depth: Float,
    behind: Float = 0.55f,
): Float = behind + (1f - behind) * (depth.coerceIn(-1f, 1f) + 1f) / 2f

/** A particle's opacity at [depth]: fainter behind. */
internal fun orbitAlpha(depth: Float): Float = 0.35f + 0.65f * (depth.coerceIn(-1f, 1f) + 1f) / 2f

/**
 * A few particles that circle the element on a tipped ellipse, passing behind it and in front of it. The orbit
 * reaches [radius] times the element's half width past its center and is tipped by [tilt] degrees. Particles turn
 * clockwise or not, shrink to [depthScale] of their size behind the element, and are [content] when set, with a
 * tail of [trail] fading dots behind each.
 */
internal class OrbitRunner(
    private val colors: List<Color>,
    private val count: Int,
    private val particleSize: Dp,
    private val duration: Duration,
    private val radius: Float = ORBIT_REACH,
    private val tilt: Float = ORBIT_TILT_DEGREES,
    private val clockwise: Boolean = true,
    private val depthScale: Float = 0.55f,
    private val content: ParticleContent? = null,
    private val trail: Int = ORBIT_TRAIL,
) : FadingLoopRunner() {
    private var contentPainter: ParticlePainter? = null
    private var textMeasurer: TextMeasurer? = null

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        if (active && content != null) {
            contentPainter =
                content.painter(context.density) {
                    textMeasurer
                        ?: TextMeasurer(context.fontFamilyResolver, context.density, context.layoutDirection)
                            .also { textMeasurer = it }
                }
        }
        super.setActive(context, active)
    }

    override fun ContentDrawScope.drawEffect() {
        val s = strength
        // Reduced motion leaves the element alone: the particles only make sense moving.
        if (s <= 0f || isReduced) {
            drawContent()
            return
        }
        val turn = loopPhase(time, duration.inWholeMilliseconds / 1000.0)
        drawParticles(turn, s, front = false)
        drawContent()
        drawParticles(turn, s, front = true)
    }

    private fun DrawScope.drawParticles(
        turn: Float,
        strength: Float,
        front: Boolean,
    ) {
        val particleRadius = particleSize.toPx() / 2
        val radiusX = size.width / 2 * radius + particleRadius
        val radiusY = radiusX * FLATTEN
        val tiltRadians = tilt * PI.toFloat() / 180f
        val sign = if (clockwise) 1f else -1f
        // The orbit's faint track: its far half behind the element, its near half in front.
        rotate(tilt, pivot = center) {
            drawArc(
                colors[0],
                startAngle = if (front) 0f else 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radiusX, center.y - radiusY),
                size = Size(2 * radiusX, 2 * radiusY),
                alpha = TRACK_ALPHA * strength,
                style = Stroke(particleRadius / 4),
            )
        }
        for (i in 0 until count) {
            val angle = 2 * PI.toFloat() * (sign * turn + i.toFloat() / count)
            val color = colors[i % colors.size]
            // A short trail of fading dots behind each particle, then the particle with its glow.
            for (j in trail downTo 0) {
                val a = angle - sign * j * TRAIL_STEP
                val depth = orbitDepth(a)
                if ((depth >= 0f) != front) continue
                val at = center + orbitPosition(a, radiusX, radiusY, tiltRadians)
                val scale = orbitScale(depth, depthScale) * (1f - j * 0.15f)
                val alpha = orbitAlpha(depth) * strength * (1f - j / (trail + 1f))
                val painter = contentPainter
                if (painter != null) {
                    translate(at.x, at.y) {
                        scale(scale, scale, pivot = Offset.Zero) {
                            with(painter) { drawParticle(i, if (j == 0) alpha else alpha * 0.5f) }
                        }
                    }
                    continue
                }
                val r = particleRadius * scale
                if (j == 0) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(color.copy(alpha = color.alpha * 0.45f), Color.Transparent),
                            at,
                            r * 2.4f,
                        ),
                        r * 2.4f,
                        at,
                        alpha,
                    )
                }
                drawCircle(color, r, at, if (j == 0) alpha else alpha * 0.5f)
            }
        }
    }

    private companion object {
        const val FLATTEN = 0.34f
        const val TRACK_ALPHA = 0.16f
        const val TRAIL_STEP = 0.09f
    }
}

// How far the orbit reaches past the element's sides, as a share of its half width.
internal const val ORBIT_REACH = 1.6f

// The default tilt: 0.3 radians counterclockwise, in degrees.
internal const val ORBIT_TILT_DEGREES = -0.3f * 180f / 3.1415927f
internal const val ORBIT_TRAIL = 4
