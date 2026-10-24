// Anvil timings (a fall in the first tenth, then a shake of 2·sin(3πt)·(1 − t) across and 4·sin(4πt)·(1 − t)
// down) are derived from Pow (https://github.com/EmergeTools/Pow), MIT, see NOTICE.
package com.jantursky.kapow.transitions

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.TransitionKind
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.TransitionRunner
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

/** The pop's content scale at [t]: up from nothing with an overshoot, settled by [POP_GROW]. */
internal fun popScale(t: Float): Float {
    val x = (t / POP_GROW).coerceIn(0f, 1f) - 1f
    return 1f + 2.7f * x * x * x + 1.7f * x * x
}

private const val ANVIL_FALL = 0.1f
private const val POP_GROW = 0.35f
private const val POP_RING = 0.6f

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

    private val kind: TransitionKind get() = if (entering()) enter else exit

    override val needsLayer: Boolean get() = true

    override fun DrawScope.configureLayer(layer: GraphicsLayer) {
        val kind = kind
        val p = progress().coerceIn(0f, 1f)
        layer.compositingStrategy =
            if (kind.isMask() && p > 0f && p < 1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        layer.translationX = 0f
        layer.translationY = 0f
        if (kind == TransitionKind.Anvil) {
            val shake = anvilShake(p)
            layer.translationX = shake.x.dp.toPx()
            layer.translationY = shake.y.dp.toPx() - anvilDrop(p) * max(size.height * 2f, 200.dp.toPx())
        }
    }

    override fun ContentDrawScope.drawEffect() {
        val kind = kind
        val p = progress().coerceIn(0f, 1f)
        if (p >= 1f || kind == TransitionKind.None || kind == TransitionKind.Anvil) {
            drawContent()
            return
        }
        // Hidden. Masks must not cut here: without an offscreen layer the cut would reach whatever is behind.
        if (p <= 0f) return
        when (kind) {
            is TransitionKind.Iris -> {
                drawContent()
                cutOutside(irisPath(size, kind.origin, p))
            }

            is TransitionKind.Clock -> {
                drawContent()
                cutOutside(clockPath(size, kind.origin, p))
            }

            is TransitionKind.Blinds -> {
                drawContent()
                cutOutside(blindsPath(size, kind.slatWidth.toPx(), kind.orientation, kind.staggered, p))
            }

            is TransitionKind.Wipe -> {
                drawContent()
                cutWipe(kind.angle, kind.glare, p)
            }

            is TransitionKind.Pop -> {
                drawRing(kind.color, p)
                scale(popScale(p)) { this@drawEffect.drawContent() }
            }

            // The content goes at once; the puff is all that is left.
            is TransitionKind.Poof -> {}

            TransitionKind.None, TransitionKind.Anvil -> {
                drawContent()
            }
        }
    }

    private fun ContentDrawScope.drawRing(
        color: Color,
        p: Float,
    ) {
        val r = p / POP_RING
        if (r >= 1f) return
        val base = min(size.width, size.height) / 2
        val radius = base * (0.6f + 0.9f * r)
        drawCircle(color, radius, center, alpha = 1f - r, style = Stroke(width = 6.dp.toPx() * (1f - r)))
    }

    override fun attach(context: EffectContext) {
        watcher?.cancel()
        if (enter !is TransitionKind.Pop && enter != TransitionKind.Anvil && exit !is TransitionKind.Poof) return
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
                            context.emit(poof(context, exit.color))
                        }

                        enter is TransitionKind.Pop && entering && was == 0f && p > 0f -> {
                            context.emit(popBurst(context, enter.color))
                        }

                        enter == TransitionKind.Anvil && entering && was < ANVIL_FALL && p >= ANVIL_FALL -> {
                            dust(context).forEach(context::emit)
                        }
                    }
                }
            }
    }

    override fun stop() {
        watcher?.cancel()
    }
}

private fun TransitionKind.isMask(): Boolean =
    this is TransitionKind.Iris || this is TransitionKind.Clock || this is TransitionKind.Wipe ||
        this is TransitionKind.Blinds

private fun EffectContext.center(): Offset = Offset(size.width / 2f, size.height / 2f)

private fun poof(
    context: EffectContext,
    color: Color,
): ParticleBatch =
    with(context.density) {
        val spread = min(context.size.width, context.size.height) * 0.3f
        ParticleBatch(
            ParticleSystem(
                Emission(
                    count = 10,
                    angle = Spread(0f, 360f),
                    speed = Spread(30.dp.toPx(), 90.dp.toPx()),
                    lifetime = Spread(0.35f, 0.45f),
                    drag = 0.1f,
                    scale = Spread(0.7f, 1.1f),
                    offsetX = Spread(-spread, spread),
                    offsetY = Spread(-spread, spread),
                ),
                seed = 11,
            ),
            puffPainter(color, 14.dp.toPx()),
            opacity = { 1f - it * it },
            growth = { 0.6f + it },
        ).apply { origin = context.center() }
    }

private fun popBurst(
    context: EffectContext,
    color: Color,
): ParticleBatch =
    with(context.density) {
        ParticleBatch(
            ParticleSystem(
                Emission(
                    count = 12,
                    angle = Spread(0f, 360f),
                    speed = Spread(160.dp.toPx(), 260.dp.toPx()),
                    lifetime = Spread(0.5f, 0.8f),
                    drag = 0.05f,
                    scale = Spread(0.6f, 1f),
                ),
                seed = 12,
            ),
            shapePainter(ParticleShape.Circle, listOf(color), 7.dp.toPx()),
        ).apply { origin = context.center() }
    }

/** Two clouds of dust that run out sideways from the bottom edge as the anvil lands. */
private fun dust(context: EffectContext): List<ParticleBatch> =
    with(context.density) {
        listOf(180f, 0f).mapIndexed { side, angle ->
            ParticleBatch(
                ParticleSystem(dustEmission(angle, this), seed = 20 + side),
                puffPainter(Color(0xFFB0B0B0), 9.dp.toPx()),
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
    density: Density,
): Emission =
    with(density) {
        Emission(
            count = 6,
            angle = Spread(angle - 15f, angle + 15f),
            speed = Spread(60.dp.toPx(), 140.dp.toPx()),
            lifetime = Spread(0.6f, 1f),
            gravity = -20.dp.toPx(),
            drag = 0.2f,
            scale = Spread(0.6f, 1f),
        )
    }
