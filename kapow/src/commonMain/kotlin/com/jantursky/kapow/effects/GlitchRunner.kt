package com.jantursky.kapow.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toIntSize
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration

/**
 * One look of the glitch, held for [GLITCH_STEP] s: how far the red and blue channels split apart, and horizontal
 * slices of an element [height] px tall, some of them pushed sideways. [offset] is the largest split in pixels;
 * slices jump up to one and a half times as far. The same [seed] and [step] always give the same look.
 */
internal fun glitchFrame(
    seed: Int,
    step: Int,
    height: Float,
    offset: Float,
): GlitchRunner.Frame {
    val random = Random(seed * 7919 + step)
    val sign = if (random.nextBoolean()) 1f else -1f
    val split = sign * offset * (0.4f + 0.6f * random.nextFloat())
    val slices = mutableListOf<GlitchRunner.Slice>()
    var top = 0f
    while (top < height) {
        // Whole pixels, so neighbouring slices meet without a soft seam.
        val next = (top + height * (0.08f + 0.22f * random.nextFloat())).roundToInt().toFloat()
        val bottom = minOf(height, maxOf(top + 1f, next))
        val jumps = random.nextFloat() < 0.5f
        val direction = if (random.nextBoolean()) 1f else -1f
        val shift = if (jumps) direction * offset * (0.3f + 1.2f * random.nextFloat()) else 0f
        slices += GlitchRunner.Slice(top, bottom, shift)
        top = bottom
    }
    return GlitchRunner.Frame(split, slices)
}

/** How long each look of the glitch is held before the next one, in seconds. */
internal const val GLITCH_STEP = 0.05f

/** Keeps one color channel of what is drawn, so three copies added together rebuild the original. */
private fun channelPaint(channel: Int): Paint =
    Paint().apply {
        val m = FloatArray(20)
        m[channel * 5 + channel] = 1f
        m[18] = 1f
        colorFilter = ColorFilter.colorMatrix(ColorMatrix(m))
        blendMode = BlendMode.Plus
    }

/**
 * Splits the element into offset red, green and blue copies and shifts horizontal slices of it sideways for
 * [duration], then snaps back. The content is drawn once into a layer and that layer is drawn several times.
 * With reduced motion the element briefly brightens instead.
 */
internal class GlitchRunner(
    private val duration: Duration,
    private val offset: Dp,
) : ChangeRunner() {
    private val time = Animatable(0f)
    private val flash = Flash()
    private val channels = Array(3) { channelPaint(it) }
    private var job: Job? = null
    private var graphics: GraphicsContext? = null
    private var content: GraphicsLayer? = null
    private var composite: GraphicsLayer? = null
    private var seed = 0
    private var frame: Frame? = null
    private var frameStep = -1

    override fun ContentDrawScope.drawEffect() {
        val graphics = graphics
        val running = time.isRunning
        if (graphics == null || (!running && !flash.isVisible)) {
            drawContent()
            return
        }
        // The node's own layer can't be used here: the content has to be drawn into a layer of this runner's, so it
        // can be drawn again several times, and the copies have to be added up in a buffer of their own.
        val content =
            this@GlitchRunner.content ?: graphics.createGraphicsLayer().also { this@GlitchRunner.content = it }
        // Recording scopes get a density of their own; borrowing this scope's would loop back into it.
        val density = Density(density, fontScale)
        val bounds = size.toIntSize()
        val outer = this
        content.record(density, layoutDirection, bounds) {
            // The content is drawn by the node's scope, so point that scope at the layer's canvas while recording.
            val context = outer.drawContext
            val canvas = context.canvas
            context.canvas = drawContext.canvas
            try {
                outer.drawContent()
            } finally {
                context.canvas = canvas
            }
        }
        val frame = if (running) frameAt(time.value, size.height, offset.toPx()) else null
        val composite =
            this@GlitchRunner.composite ?: graphics.createGraphicsLayer().also {
                it.compositingStrategy = CompositingStrategy.Offscreen
                this@GlitchRunner.composite = it
            }
        composite.record(density, layoutDirection, bounds) {
            if (frame == null) {
                drawLayer(content)
                flash.draw(this)
                return@record
            }
            for (slice in frame.slices) {
                clipRect(0f, slice.top, size.width, slice.bottom) {
                    translate(left = slice.shift) { drawChannels(content, frame.split) }
                }
            }
        }
        drawLayer(composite)
    }

    private fun frameAt(
        t: Float,
        height: Float,
        offset: Float,
    ): Frame {
        val step = (t * duration.inWholeMilliseconds / 1000f / GLITCH_STEP).toInt()
        val cached = frame
        if (cached != null && step == frameStep) return cached
        frameStep = step
        return glitchFrame(seed, step, height, offset).also { frame = it }
    }

    private fun DrawScope.drawChannels(
        layer: GraphicsLayer,
        split: Float,
    ) {
        val bounds = Rect(0f, 0f, size.width, size.height)
        for (channel in 0 until 3) {
            // Red goes one way, blue the other, green stays.
            translate(left = split * (1 - channel)) {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(bounds, channels[channel])
                    drawLayer(layer)
                    canvas.restore()
                }
            }
        }
    }

    override fun attach(context: EffectContext) {
        graphics = context.graphicsContext
    }

    override fun fire(context: EffectContext) {
        job?.cancel()
        if (context.isReducedMotion()) {
            job = context.scope.launch { flash.play() }
            return
        }
        seed++
        frameStep = -1
        val millis = duration.inWholeMilliseconds.toInt()
        job =
            context.scope.launch {
                time.snapTo(0f)
                time.animateTo(1f, tween(millis, easing = LinearEasing))
                time.snapTo(0f)
            }
    }

    override fun stop() {
        job?.cancel()
        content?.let { graphics?.releaseGraphicsLayer(it) }
        composite?.let { graphics?.releaseGraphicsLayer(it) }
        content = null
        composite = null
    }

    /** A horizontal band from [top] to [bottom], drawn [shift] px to the side. */
    internal data class Slice(
        val top: Float,
        val bottom: Float,
        val shift: Float,
    )

    /** One look of the glitch: the channel [split] and the [slices] that cover the element from top to bottom. */
    internal data class Frame(
        val split: Float,
        val slices: List<Slice>,
    )
}
