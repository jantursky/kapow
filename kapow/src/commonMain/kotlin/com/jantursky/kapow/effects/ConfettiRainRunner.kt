package com.jantursky.kapow.effects

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ParticlePhysics
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.applyTo
import com.jantursky.kapow.internal.emitterOrigin
import com.jantursky.kapow.internal.growthCurve
import com.jantursky.kapow.internal.opacityCurve
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticlePainter
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.pickColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.floor

/** The most pieces that are falling at once, however high the density. */
internal const val MAX_RAIN_PIECES = 200

/**
 * How many pieces to emit now, and what to carry over to the next frame. [carry] is the part of a piece left from
 * earlier frames; [density] pieces per second come in over [seconds].
 */
internal fun rainPieces(
    carry: Float,
    density: Float,
    seconds: Float,
): Pair<Int, Float> {
    val total = carry + density * seconds
    val whole = floor(total)
    return whole.toInt() to total - whole
}

/** How many of [wanted] pieces fit under [cap] when [live] are already falling. */
internal fun rainBudget(
    live: Int,
    wanted: Int,
    cap: Int = MAX_RAIN_PIECES,
): Int = wanted.coerceAtMost(cap - live).coerceAtLeast(0)

/**
 * [count] pieces of confetti starting along the top of an element [width] px wide, drifting down and sideways while
 * gravity speeds them up and drag keeps them fluttering rather than dropping.
 */
internal fun rainEmission(
    count: Int,
    width: Float,
    density: Density,
): Emission =
    with(density) {
        Emission(
            count = count,
            angle = Spread(90f - 14f, 90f + 14f),
            speed = Spread(20.dp.toPx(), 70.dp.toPx()),
            lifetime = Spread(1.8f, 2.8f),
            gravity = 180.dp.toPx(),
            drag = 0.3f,
            spin = Spread(-360f, 360f),
            rotation = Spread(0f, 360f),
            scale = Spread(0.7f, 1.1f),
            offsetX = Spread(-width / 2f, width / 2f),
            wobble = 6.dp.toPx(),
            wobbleRate = 1.2f,
        )
    }

/**
 * Confetti that keeps falling from the element's top edge while active. The element tracks how many pieces it has
 * in the air and holds back new ones at [MAX_RAIN_PIECES]. When it turns off the pieces still falling run out their
 * lifetime and nothing runs after that. Reduced motion drops none.
 */
internal class ConfettiRainRunner(
    private val colors: List<Color>,
    private val density: Float,
    private val size: Dp,
    private val physics: ParticlePhysics = ParticlePhysics.Default,
    private val firstSeed: Int = 0,
) : ConditionalRunner() {
    private var job: Job? = null
    private var seed = firstSeed

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        job?.cancel()
        if (!active || context.isReducedMotion()) return
        val speed = context.speed
        val unit = with(context.density) { size.toPx() }
        val opacity = physics.opacityCurve(::confettiOpacity)
        val growth = physics.growthCurve { 1f }
        job =
            context.scope.launch {
                // Pieces in the air, as the second each batch is gone and how many it holds.
                val inAir = ArrayDeque<Pair<Double, Int>>()
                var live = 0
                var now = 0.0
                var last = withFrameNanos { it }
                var carry = 0f
                var sinceBatch = 0f
                while (isActive) {
                    val frame = withFrameNanos { it }
                    // A long frame (a stall, a tab in the background) counts for at most 1/30 s.
                    val seconds = ((frame - last) / 1e9).coerceAtMost(MAX_STEP)
                    last = frame
                    now += seconds
                    while (inAir.isNotEmpty() && inAir.first().first <= now) live -= inAir.removeFirst().second
                    val (due, rest) = rainPieces(carry, density * speed, seconds.toFloat())
                    carry = rest
                    sinceBatch += seconds.toFloat()
                    // Small batches every few frames rather than one piece at a time.
                    if (due == 0 || (sinceBatch < MIN_BATCH_GAP && due < BATCH)) {
                        carry += due
                        continue
                    }
                    val count = rainBudget(live, due)
                    // Pieces held back by the cap are dropped, not saved up, so the rain never bursts after a pause.
                    if (count == 0) continue
                    sinceBatch = 0f
                    val width = context.size.width.toFloat()
                    val height = context.size.height.toFloat()
                    val emission =
                        physics.applyTo(
                            rainEmission(count, width, context.density),
                            context.density.density,
                            Size(width, height),
                            context.layoutDirection,
                        )
                    val n = seed++
                    val system = ParticleSystem(emission, n)
                    val pieceColors = pickColors(colors, physics.colorPick, count, n)
                    val painter =
                        ParticlePainter { i, alpha ->
                            val piece = confettiPiece(i + n, unit)
                            val w = piece.width * confettiFlip(system.rotation[i], (i + n) * 1.7f)
                            drawRect(
                                pieceColors[i % pieceColors.size],
                                Offset(-w / 2, -piece.height / 2),
                                Size(w, piece.height),
                                alpha,
                            )
                        }
                    val batch = ParticleBatch(system, painter, opacity = opacity, growth = growth)
                    batch.origin = physics.emitterOrigin(width, height, Offset(width / 2f, 0f))
                    batch.timeScale = speed
                    batch.blendMode = physics.blendMode
                    context.emit(batch)
                    live += count
                    inAir.addLast((now + emission.lifetime.max / speed) to count)
                }
            }
    }

    override fun stop() {
        job?.cancel()
    }

    private companion object {
        const val MAX_STEP = 1.0 / 30
        const val MIN_BATCH_GAP = 0.08f
        const val BATCH = 4
    }
}
