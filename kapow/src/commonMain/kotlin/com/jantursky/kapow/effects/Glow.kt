// The glow spring (damping 0.75, stiffness 15), its impulse of 5 with +1.5 per repeat, and the brightening of
// a quarter of the halo strength are derived from Pow (https://github.com/EmergeTools/Pow), MIT, see NOTICE.
package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ReducedMotionMode
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.EffectRunner
import com.jantursky.kapow.internal.ProgressRunner
import com.jantursky.kapow.internal.at
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The velocity a glow impulse gives its spring: a fresh 5, or 1.5 more while it is still glowing, up to 5. */
internal fun glowImpulse(
    running: Boolean,
    velocity: Float,
    cap: Float = 5f,
): Float = if (running) min(velocity + 1.5f, cap) else min(5f, cap)

/** Shared by the three glows: a halo behind the element, and the element brightened by a quarter of it. */
internal abstract class GlowRunnerBase(
    color: Color,
    radius: Dp,
    shape: Shape,
    private val offset: DpOffset = DpOffset.Zero,
    private val limit: Float = MAX_HALO,
    private val inner: InnerGlow? = null,
    private val brightening: Float = BRIGHTENING,
) : EffectRunner() {
    private val halo = Halo(color, radius, shape, limit = limit)
    private var graphics: GraphicsContext? = null
    private var content: GraphicsLayer? = null

    protected abstract val amount: Float

    override fun ContentDrawScope.drawEffect() {
        val amount = amount
        drawHaloAndContent(amount)
        inner?.draw(this, amount)
    }

    private fun ContentDrawScope.drawHaloAndContent(amount: Float) {
        if (offset == DpOffset.Zero) {
            halo.draw(this, amount)
        } else {
            translate(offset.x.toPx(), offset.y.toPx()) { halo.draw(this, amount) }
        }
        val brighten = min(amount, limit) / limit * brightening
        val graphics = graphics
        if (brighten <= 0f || graphics == null) {
            drawContent()
            return
        }
        // Brightening only the element's own pixels needs them in a buffer of their own. It is a separate layer so
        // the halo, which reaches outside the element, isn't clipped to it.
        val layer =
            content ?: graphics.createGraphicsLayer().also {
                it.compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
                content = it
            }
        layer.record {
            this@drawHaloAndContent.drawContent()
            drawRect(Color.White, alpha = brighten, blendMode = BlendMode.SrcAtop)
        }
        drawLayer(layer)
    }

    override fun attach(context: EffectContext) {
        graphics = context.graphicsContext
        halo.attach(context.graphicsContext)
    }

    override fun stop() {
        halo.release()
        content?.let { graphics?.releaseGraphicsLayer(it) }
        content = null
    }
}

internal class GlowRunner(
    color: Color,
    radius: Dp,
    shape: Shape,
    intensity: Float = 1f,
    spread: Dp = 0.dp,
    offset: DpOffset = DpOffset.Zero,
    innerGlow: Float = 0f,
    private val flare: Duration? = null,
    private val decay: Duration? = null,
    private val stackLimit: Float = MAX_HALO,
) : ChangeRunner() {
    private val shown = if (intensity == 1f) color else color.copy(alpha = color.alpha * intensity)
    private val base =
        object : GlowRunnerBase(
            shown,
            radius + spread,
            shape,
            offset,
            stackLimit,
            if (innerGlow > 0f) InnerGlow(shown, radius + spread, shape, innerGlow, stackLimit) else null,
        ) {
            override val amount: Float get() = glow.value.coerceAtLeast(0f) * SPRING_GAIN
        }
    private val glow = Animatable(0f)
    private var job: Job? = null

    override fun ContentDrawScope.drawEffect() = with(base) { drawEffect() }

    override fun attach(context: EffectContext) = base.attach(context)

    override fun fire(context: EffectContext) {
        job?.cancel()
        val speed = context.speed
        job =
            context.scope.launch {
                if (context.isReducedMotion()) {
                    if (!context.showsReducedStandIn()) return@launch
                    // A faint, short halo that doesn't pulse.
                    glow.snapTo(0.3f)
                    glow.animateTo(
                        0f,
                        tween(
                            300
                                .milliseconds
                                .at(speed)
                                .inWholeMilliseconds
                                .toInt(),
                        ),
                    )
                } else if (flare == null && decay == null) {
                    val cap = 5f * stackLimit / MAX_HALO
                    val velocity = glowImpulse(glow.isRunning, glow.velocity, cap)
                    // A faster spring is a stiffer one: time shrinks with the square root of stiffness.
                    val stiffness = 15f * speed * speed
                    glow.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = stiffness), initialVelocity = velocity)
                } else {
                    val rise = (flare ?: DEFAULT_FLARE).at(speed).inWholeMilliseconds.toInt()
                    val fall = (decay ?: DEFAULT_DECAY).at(speed).inWholeMilliseconds.toInt()
                    // A change while still glowing builds on what is showing, up to the limit.
                    val now = glow.value.coerceAtLeast(0f) * SPRING_GAIN
                    val peak = min(if (now > 0.05f) now + 0.5f else 1f, stackLimit)
                    glow.animateTo(peak / SPRING_GAIN, tween(rise))
                    glow.animateTo(0f, tween(fall))
                }
            }
    }

    override fun stop() {
        job?.cancel()
        base.stop()
    }

    private companion object {
        // The spring above peaks at about 0.57 after one impulse; this makes one change a full halo and leaves
        // room for quick repeats to build up to the cap.
        const val SPRING_GAIN = 1.75f
        val DEFAULT_FLARE = 150.milliseconds
        val DEFAULT_DECAY = 1000.milliseconds
    }
}

/** How strong the halo is at [phase] (in turns) of a breathing cycle, as a factor on its steady strength. */
internal fun glowBreath(
    phase: Float,
    amplitude: Float,
): Float = (1f + amplitude * sin(2f * PI.toFloat() * phase)).coerceAtLeast(0f)

/**
 * A steady halo. It fades in and out, swells and shrinks by [breathe] (a share of its strength) once every
 * [period] when [breathe] is above 0, and brightens the element by [brighten] of the halo's strength.
 */
internal class ConditionalGlowRunner(
    color: Color,
    radius: Dp,
    shape: Shape,
    private val steady: Float = STEADY,
    private val breathe: Float = 0f,
    private val period: Duration = 2.seconds,
    brighten: Float = BRIGHTENING,
) : ConditionalRunner() {
    private val strength = Animatable(0f)
    private var clock by mutableDoubleStateOf(0.0)
    private var clockJob: Job? = null
    private val base =
        object : GlowRunnerBase(color, radius, shape, brightening = brighten) {
            override val amount: Float
                get() =
                    if (breathe > 0f) {
                        strength.value * glowBreath(loopPhase(clock, period.inWholeMilliseconds / 1000.0), breathe)
                    } else {
                        strength.value
                    }
        }
    private var job: Job? = null

    override fun ContentDrawScope.drawEffect() = with(base) { drawEffect() }

    override fun attach(context: EffectContext) = base.attach(context)

    override fun setActive(
        context: EffectContext,
        active: Boolean,
    ) {
        job?.cancel()
        val reduced = context.isReducedMotion()
        // Reduced motion keeps the steady halo but lets it appear without a fade, or with one in the Fade mode.
        val fades = !reduced || context.reducedMotionMode == ReducedMotionMode.Fade
        val chosen = if (active) context.fadeIn else context.fadeOut
        val millis =
            chosen?.inWholeMilliseconds?.toInt() ?: 250
                .milliseconds
                .at(context.speed)
                .inWholeMilliseconds
                .toInt()
        if (active && breathe > 0f && !reduced) startClock(context)
        job =
            context.scope.launch {
                strength.animateTo(if (active) steady else 0f, tween(if (fades) millis else 0))
                if (!active) clockJob?.cancel()
            }
    }

    private fun startClock(context: EffectContext) {
        if (clockJob?.isActive == true) return
        val speed = context.speed
        clockJob =
            context.scope.launch {
                var last = -1L
                while (true) {
                    withFrameNanos { now ->
                        if (last >= 0) clock += ((now - last) / 1e9).coerceAtMost(1.0 / 30) * speed
                        last = now
                    }
                }
            }
    }

    override fun stop() {
        job?.cancel()
        clockJob?.cancel()
        base.stop()
    }
}

private const val STEADY = 0.7f
private const val BRIGHTENING = 0.25f
