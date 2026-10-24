package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
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

/** A particle's size factor at [depth]: smaller behind, full size in front. */
internal fun orbitScale(depth: Float): Float = 0.55f + 0.45f * (depth.coerceIn(-1f, 1f) + 1f) / 2f

/** A particle's opacity at [depth]: fainter behind. */
internal fun orbitAlpha(depth: Float): Float = 0.35f + 0.65f * (depth.coerceIn(-1f, 1f) + 1f) / 2f

/** A few particles that circle the element on a tipped ellipse, passing behind it and in front of it. */
internal class OrbitRunner(
    private val colors: List<Color>,
    private val count: Int,
    private val particleSize: Dp,
    private val duration: Duration,
) : FadingLoopRunner() {
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
        val radius = particleSize.toPx() / 2
        val radiusX = size.width / 2 * REACH + radius
        val radiusY = radiusX * FLATTEN
        // The orbit's faint track: its far half behind the element, its near half in front.
        rotate(TILT * 180f / PI.toFloat(), pivot = center) {
            drawArc(
                colors[0],
                startAngle = if (front) 0f else 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radiusX, center.y - radiusY),
                size = Size(2 * radiusX, 2 * radiusY),
                alpha = TRACK_ALPHA * strength,
                style = Stroke(radius / 4),
            )
        }
        for (i in 0 until count) {
            val angle = 2 * PI.toFloat() * (turn + i.toFloat() / count)
            val color = colors[i % colors.size]
            // A short trail of fading dots behind each particle, then the particle with its glow.
            for (j in TRAIL downTo 0) {
                val a = angle - j * TRAIL_STEP
                val depth = orbitDepth(a)
                if ((depth >= 0f) != front) continue
                val at = center + orbitPosition(a, radiusX, radiusY, TILT)
                val r = radius * orbitScale(depth) * (1f - j * 0.15f)
                val alpha = orbitAlpha(depth) * strength * (1f - j / (TRAIL + 1f))
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
        // How far the orbit reaches past the element's sides, as a share of its half width.
        const val REACH = 1.6f
        const val FLATTEN = 0.34f
        const val TILT = -0.3f
        const val TRACK_ALPHA = 0.16f
        const val TRAIL = 4
        const val TRAIL_STEP = 0.09f
    }
}
