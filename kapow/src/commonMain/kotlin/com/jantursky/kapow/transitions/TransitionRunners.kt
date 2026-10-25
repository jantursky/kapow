// Anvil timings (a fall in the first tenth, then a shake of 2·sin(3πt)·(1 − t) across and 4·sin(4πt)·(1 − t)
// down) are derived from Pow (https://github.com/EmergeTools/Pow), MIT, see NOTICE.
package com.jantursky.kapow.transitions

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.TransitionKind
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.TransitionRunner
import com.jantursky.kapow.internal.applyTo
import com.jantursky.kapow.internal.growthCurve
import com.jantursky.kapow.internal.opacityCurve
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.puffPainter
import com.jantursky.kapow.particles.shapePainter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal fun createRunner(
    enter: TransitionKind,
    exit: TransitionKind,
    reduced: Boolean,
): TransitionRunner = if (reduced) FadeRunner(enter, exit) else KindRunner(enter, exit)

/** The anvil's drop at [t] (0 to 1 over the whole transition): 1 is a full fall height above, 0 is landed. */
internal fun anvilDrop(t: Float): Float {
    if (t >= ANVIL_FALL) return 0f
    val f = (t / ANVIL_FALL).coerceIn(0f, 1f)
    return 1f - f * f
}

/** The anvil's shake after landing, in dp, at [t] (0 to 1 over the whole transition). */
internal fun anvilShake(t: Float): Offset {
    if (t < ANVIL_FALL) return Offset.Zero
    val u = ((t - ANVIL_FALL) / (1f - ANVIL_FALL)).coerceIn(0f, 1f)
    return Offset(2f * sin(3f * PI.toFloat() * u) * (1f - u), 4f * sin(4f * PI.toFloat() * u) * (1f - u))
}

/**
 * The pop's content scale at [t]: up from nothing with an overshoot, settled by [POP_GROW]. [overshoot] scales the
 * overshoot: 1 is the usual one, 0 grows to full size without passing it, and more passes it further.
 */
internal fun popScale(
    t: Float,
    overshoot: Float = 1f,
): Float {
    val x = (t / POP_GROW).coerceIn(0f, 1f) - 1f
    if (overshoot == 1f) return 1f + 2.7f * x * x * x + 1.7f * x * x
    val c = 1.7f * overshoot
    return 1f + (c + 1f) * x * x * x + c * x * x
}

/**
 * How far the ring [index] has grown at [progress], from 0 (not started) to 1 (gone). Rings follow one another
 * [POP_RING_GAP] of the transition apart, and the last one is gone by the end for up to [MAX_POP_RINGS].
 */
internal fun popRing(
    index: Int,
    progress: Float,
): Float = (progress - index * POP_RING_GAP) / POP_RING

internal const val MAX_POP_RINGS = 5
private const val ANVIL_FALL = 0.1f
private const val POP_GROW = 0.35f
private const val POP_RING = 0.6f
private const val POP_RING_GAP = 0.1f

/** With reduced motion every transition is a fade over its progress. */
private class FadeRunner(
    private val enter: TransitionKind,
    private val exit: TransitionKind,
) : TransitionRunner() {
    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val kind = if (entering()) enter else exit
        layer.alpha = if (kind == TransitionKind.None) 1f else progress().coerceIn(0f, 1f)
    }
}

private class KindRunner(
    private val enter: TransitionKind,
    private val exit: TransitionKind,
) : TransitionRunner() {
    private var watcher: Job? = null
    private val enterPart = partFor(enter)
    private val exitPart = partFor(exit)

    private val kind: TransitionKind get() = if (entering()) enter else exit

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val kind = kind
        val p = progress().coerceIn(0f, 1f)
        layer.compositingStrategy =
            if (kind.isMask() && p > 0f && p < 1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        layer.translationX = 0f
        layer.translationY = 0f
        layer.alpha = 1f
        if (kind is TransitionKind.Anvil) {
            val shake = anvilShake(p)
            val drop = if (kind.dropHeight.isSpecified) kind.dropHeight.toPx() else max(size.height * 2f, 200.dp.toPx())
            layer.translationX = (shake.x * kind.shake).dp.toPx()
            layer.translationY = (shake.y * kind.shake).dp.toPx() - anvilDrop(p) * drop
        }
        val part = if (entering()) enterPart else exitPart
        if (part != null && p > 0f && p < 1f) part.configureLayer(layer, p)
    }

    override fun ContentDrawScope.drawEffect() {
        val kind = kind
        val p = progress().coerceIn(0f, 1f)
        if (p >= 1f || kind == TransitionKind.None || kind is TransitionKind.Anvil) {
            drawContent()
            return
        }
        // Hidden. Masks must not cut here: without an offscreen layer the cut would reach whatever is behind.
        if (p <= 0f) return
        when (kind) {
            is TransitionKind.Iris -> {
                drawContent()
                cutIris(kind.origin, kind.softEdge.toPx(), p)
            }

            is TransitionKind.Clock -> {
                drawContent()
                cutClock(kind.origin, kind.softEdge.toPx(), kind.counterClockwise, p)
            }

            is TransitionKind.Blinds -> {
                drawContent()
                cutOutside(
                    blindsPath(
                        size,
                        kind.slatWidth.toPx(),
                        kind.orientation,
                        kind.staggered,
                        p,
                        kind.slatCount,
                        kind.staggerAmount,
                        kind.reverse,
                    ),
                )
            }

            is TransitionKind.Wipe -> {
                drawContent()
                val width = if (kind.glareWidth.isSpecified) kind.glareWidth.toPx() else Float.NaN
                cutWipe(kind.angle, kind.glare, p, kind.softEdge.toPx(), width, kind.intensity)
            }

            is TransitionKind.Pop -> {
                for (ring in 0 until kind.ringCount) drawRing(kind.color, popRing(ring, p))
                scale(popScale(p, kind.overshoot)) { this@drawEffect.drawContent() }
            }

            // The content goes at once; the puff is all that is left.
            is TransitionKind.Poof -> {}

            TransitionKind.None, is TransitionKind.Anvil -> {
                drawContent()
            }

            else -> {
                with(checkNotNull(if (entering()) enterPart else exitPart)) { draw(p) }
            }
        }
    }

    /** Draws one ring of the pop, [r] of the way through its life: 0 not started, 1 gone. */
    private fun ContentDrawScope.drawRing(
        color: Color,
        r: Float,
    ) {
        if (r < 0f || r >= 1f) return
        val base = min(size.width, size.height) / 2
        val radius = base * (0.6f + 0.9f * r)
        drawCircle(color, radius, center, alpha = 1f - r, style = Stroke(width = 6.dp.toPx() * (1f - r)))
    }

    override fun attach(context: EffectContext) {
        enterPart?.attach(context, { entering() }, { progress() })
        exitPart?.attach(context, { entering() }, { progress() })
        watcher?.cancel()
        if (enter !is TransitionKind.Pop && enter !is TransitionKind.Anvil && exit !is TransitionKind.Poof) return
        watcher =
            context.scope.launch {
                var last: Pair<Boolean, Float>? = null
                snapshotFlow { entering() to progress() }.collect { now ->
                    val before = last
                    last = now
                    if (before == null) return@collect
                    val (entering, p) = now
                    val (wasEntering, was) = before
                    when {
                        // The exit just started.
                        exit is TransitionKind.Poof && !entering && wasEntering && p > 0f -> {
                            context.emit(poof(context, exit))
                        }

                        enter is TransitionKind.Pop && entering && was == 0f && p > 0f -> {
                            context.emit(popBurst(context, enter))
                        }

                        enter is TransitionKind.Anvil && entering && was < ANVIL_FALL && p >= ANVIL_FALL -> {
                            dust(context, enter).forEach(context::emit)
                        }
                    }
                }
            }
    }

    override fun stop() {
        watcher?.cancel()
        enterPart?.stop()
        exitPart?.stop()
    }
}

private fun TransitionKind.isMask(): Boolean =
    this is TransitionKind.Iris || this is TransitionKind.Clock || this is TransitionKind.Wipe ||
        this is TransitionKind.Blinds

private fun EffectContext.center(): Offset = Offset(size.width / 2f, size.height / 2f)

/** How wide the cone of a directed poof is, in degrees. */
private const val POOF_CONE = 90f

private fun poof(
    context: EffectContext,
    kind: TransitionKind.Poof,
): ParticleBatch =
    with(context.density) {
        val spread = min(context.size.width, context.size.height) * 0.3f
        val color = kind.color
        ParticleBatch(
            ParticleSystem(
                Emission(
                    count = kind.puffCount,
                    angle =
                        if (kind.direction == null) {
                            Spread(0f, 360f)
                        } else {
                            Spread(kind.direction - POOF_CONE / 2, kind.direction + POOF_CONE / 2)
                        },
                    speed = Spread(30.dp.toPx(), 90.dp.toPx()),
                    lifetime = Spread(0.35f, 0.45f),
                    drag = 0.1f,
                    scale = Spread(0.7f, 1.1f),
                    offsetX = Spread(-spread, spread),
                    offsetY = Spread(-spread, spread),
                ),
                seed = 11,
            ),
            puffPainter(color, kind.puffSize.toPx() / 2),
            opacity = { 1f - it * it },
            growth = { 0.6f + it },
        ).apply { origin = context.center() }
    }

private fun popBurst(
    context: EffectContext,
    kind: TransitionKind.Pop,
): ParticleBatch =
    with(context.density) {
        val base =
            Emission(
                count = kind.particleCount,
                angle = Spread(0f, 360f),
                speed = Spread(160.dp.toPx(), 260.dp.toPx()),
                lifetime = Spread(0.5f, 0.8f),
                drag = 0.05f,
                scale = Spread(0.6f, 1f),
            )
        val physics = kind.physics
        ParticleBatch(
            ParticleSystem(
                physics.applyTo(
                    base,
                    density,
                    Size(context.size.width.toFloat(), context.size.height.toFloat()),
                    context.layoutDirection,
                ),
                seed = 12,
            ),
            shapePainter(ParticleShape.Circle, listOf(kind.color), 7.dp.toPx()),
            opacity = physics.opacityCurve { 1f - it * it * it * it },
            growth = physics.growthCurve { 1f },
        ).apply { origin = context.center() }
    }

/** Two clouds of dust that run out sideways from the bottom edge as the anvil lands. */
private fun dust(
    context: EffectContext,
    kind: TransitionKind.Anvil,
): List<ParticleBatch> =
    with(context.density) {
        listOf(180f, 0f).mapIndexed { side, angle ->
            ParticleBatch(
                ParticleSystem(dustEmission(angle, kind.dustCount, this), seed = 20 + side),
                puffPainter(kind.dustColor, 9.dp.toPx()),
                opacity = { 0.8f * (1f - it) },
                growth = { 0.5f + it },
            ).apply {
                origin =
                    Offset(if (side == 0) 0f else context.size.width.toFloat(), context.size.height.toFloat())
            }
        }
    }

private fun dustEmission(
    angle: Float,
    count: Int,
    density: Density,
): Emission =
    with(density) {
        Emission(
            count = count,
            angle = Spread(angle - 15f, angle + 15f),
            speed = Spread(60.dp.toPx(), 140.dp.toPx()),
            lifetime = Spread(0.6f, 1f),
            gravity = -20.dp.toPx(),
            drag = 0.2f,
            scale = Spread(0.6f, 1f),
        )
    }
