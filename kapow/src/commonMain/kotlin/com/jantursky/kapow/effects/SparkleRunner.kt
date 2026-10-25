package com.jantursky.kapow.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.at
import com.jantursky.kapow.internal.awaitFrameTime
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.shapePainter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Where [count] stars twinkle over an element of [size], as x0, y0, x1, y1 and so on. The element is split into a
 * grid of about [count] cells (of the element's size times [spread], centered on it) and each star takes a different cell, at a seeded spot inside it, so the stars
 * cover the element without clumping. The order is shuffled too, so they don't light up row by row.
 */
internal fun sparklePlacements(
    count: Int,
    size: Size,
    seed: Int,
    spread: Float = 1f,
): FloatArray {
    val random = Random(seed)
    val area = Size(size.width * spread, size.height * spread)
    val shiftX = (size.width - area.width) / 2
    val shiftY = (size.height - area.height) / 2
    val aspect = if (area.height > 0f) area.width / area.height else 1f
    val columns = ceil(sqrt(count * aspect)).toInt().coerceIn(1, count)
    val rows = ceil(count / columns.toFloat()).toInt()
    val cells = (0 until columns * rows).shuffled(random)
    val cellWidth = area.width / columns
    val cellHeight = area.height / rows
    val points = FloatArray(count * 2)
    for (i in 0 until count) {
        val cell = cells[i]
        points[2 * i] =
            (cell % columns + CELL_INSET + (1 - 2 * CELL_INSET) * random.nextFloat()) * cellWidth + shiftX
        points[2 * i + 1] =
            (cell / columns + CELL_INSET + (1 - 2 * CELL_INSET) * random.nextFloat()) * cellHeight + shiftY
    }
    return points
}

/** How big a star is: a third of the element's shorter side, kept between [minPx] and [maxPx]. */
internal fun sparkleSize(
    size: Size,
    minPx: Float,
    maxPx: Float,
): Float = (min(size.width, size.height) / 3).coerceIn(minPx, maxPx)

/** A star's brightness over its life: it swells in and dies away again, peaking halfway. */
internal fun twinkle(progress: Float): Float = sin(PI.toFloat() * progress.coerceIn(0f, 1f))

private const val CELL_INSET = 0.15f

/** One four-point star at a time, each in its own spot over the element, until [count] have twinkled. */
internal class SparkleRunner(
    private val color: Color,
    private val count: Int,
    private val starSize: Dp = Dp.Unspecified,
    private val duration: Duration = 800.milliseconds,
    private val spread: Float = 1f,
    private val spin: ClosedFloatingPointRange<Float> = -90f..90f,
    private val shape: ParticleShape = ParticleShape.Sparkle,
    firstSeed: Int = 0,
) : ChangeRunner() {
    private val flash = Flash()
    private var job: Job? = null

    // Each change gets the next seed: a different pattern from the last, the same from run to run.
    private var seed = firstSeed

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        layer.compositingStrategy = flash.compositingStrategy
    }

    override fun ContentDrawScope.drawEffect() {
        drawContent()
        flash.draw(this)
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        if (context.isReducedMotion()) {
            if (context.showsReducedStandIn()) job = context.scope.launch { flash.play(context) }
            return
        }
        val size = Size(context.size.width.toFloat(), context.size.height.toFloat())
        val first = seed
        seed += count
        val placements = sparklePlacements(count, size, first, spread)
        val pace = duration.inWholeMilliseconds / 800f
        val star =
            STAR.copy(
                lifetime = Spread(STAR.lifetime.min * pace, STAR.lifetime.max * pace),
                spin = Spread(spin.start, spin.endInclusive),
            )
        val painter =
            with(context.density) {
                val px =
                    if (starSize == Dp.Unspecified) {
                        sparkleSize(size, MIN_SIZE.toPx(), MAX_SIZE.toPx())
                    } else {
                        starSize.toPx()
                    }
                shapePainter(shape, listOf(color), px)
            }
        val total = SPREAD * pace.toDouble()
        val gap = if (count > 1) total / (count - 1) else total
        job =
            context.scope.launch {
                for (i in 0 until count) {
                    if (i > 0) awaitFrameTime(gap.at(context.speed))
                    val batch =
                        ParticleBatch(
                            ParticleSystem(star, first + i),
                            painter,
                            opacity = { (twinkle(it) * 1.5f).coerceAtMost(1f) },
                            growth = { 0.15f + 0.85f * twinkle(it) },
                        )
                    batch.origin = Offset(placements[2 * i], placements[2 * i + 1])
                    batch.timeScale = context.speed
                    context.emit(batch)
                }
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        val SPREAD = 350.milliseconds
        val MIN_SIZE = 10.dp
        val MAX_SIZE = 22.dp

        // A star stays where it appears and turns a little while it twinkles.
        val STAR =
            Emission(
                count = 1,
                angle = Spread(0f),
                speed = Spread(0f),
                lifetime = Spread(0.4f, 0.5f),
                spin = Spread(-90f, 90f),
                rotation = Spread(-15f, 15f),
                scale = Spread(0.65f, 1f),
            )
    }
}
