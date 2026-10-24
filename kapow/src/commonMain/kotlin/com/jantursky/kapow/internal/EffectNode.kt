package com.jantursky.kapow.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntSize
import com.jantursky.kapow.KapowFeedbackEvent
import com.jantursky.kapow.LocalKapowFeedback
import com.jantursky.kapow.LocalKapowMotion
import com.jantursky.kapow.durationScale
import com.jantursky.kapow.particles.LocalParticleHost
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleField
import com.jantursky.kapow.resolveReducedMotion
import kotlinx.coroutines.CoroutineScope

/**
 * The node behind every Kapow modifier. It hosts one [EffectRunner] and draws it, inside a graphics layer of the
 * node's own when the runner asks for one, then draws the runner's particles itself when no particle layer is above
 * the element.
 */
internal abstract class EffectNode<R : EffectRunner>(
    runner: R,
) : Modifier.Node(),
    LayoutAwareModifierNode,
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode,
    EffectContext {
    var runner: R = runner
        private set

    private val particles = ParticleField()
    private var coordinates: LayoutCoordinates? = null
    private var layer: GraphicsLayer? = null

    override val shouldAutoInvalidate: Boolean get() = false

    final override var size: IntSize = IntSize.Zero
        private set

    override val scope: CoroutineScope get() = coroutineScope
    override val density: Density get() = requireDensity()
    override val layoutDirection: LayoutDirection get() = requireLayoutDirection()
    override val fontFamilyResolver: FontFamily.Resolver get() = currentValueOf(LocalFontFamilyResolver)
    override val graphicsContext: GraphicsContext get() = requireGraphicsContext()

    override fun isReducedMotion(): Boolean =
        resolveReducedMotion(currentValueOf(LocalKapowMotion), coroutineScope.coroutineContext.durationScale)

    override fun emit(batch: ParticleBatch) {
        val emitter = coordinates
        val host = currentValueOf(LocalParticleHost)
        if (emitter != null && host != null && host.add(batch, emitter)) return
        particles.add(batch, coroutineScope)
    }

    fun perform(feedback: KapowFeedbackEvent?) {
        if (feedback != null) currentValueOf(LocalKapowFeedback)?.perform(feedback)
    }

    /** A runner for the current configuration, at rest. */
    protected abstract fun newRunner(): R

    /** Called at the start of each draw. */
    protected open fun beforeDraw() {}

    /** Swaps in a new runner, for when the effect's configuration changes. */
    fun replaceRunner(runner: R = newRunner()) {
        this.runner.stop()
        this.runner = runner
        if (isAttached) {
            runner.attach(this)
            invalidateDraw()
        }
    }

    override fun onRemeasured(size: IntSize) {
        this.size = size
    }

    override fun ContentDrawScope.draw() {
        beforeDraw()
        val runner = runner
        if (runner.needsLayer) {
            // The layer wraps the runner's own drawing as well as the content, so offscreen blending and transforms
            // apply to both and never reach whatever is drawn behind the element.
            val layer = layer ?: requireGraphicsContext().createGraphicsLayer().also { layer = it }
            with(runner) { configureLayer(layer) }
            val outer = this
            // The recording scope gets a density of its own; borrowing this scope's would loop back into it.
            layer.record(Density(density, fontScale), layoutDirection, size.toIntSize()) {
                // The content is drawn by this scope, so point it at the layer's canvas while recording.
                val context = outer.drawContext
                val canvas = context.canvas
                context.canvas = drawContext.canvas
                try {
                    with(runner) { LayerContentScope(this@record, outer).drawEffect() }
                } finally {
                    context.canvas = canvas
                }
            }
            drawLayer(layer)
        } else {
            with(runner) { drawEffect() }
        }
        particles.draw(this)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun onAttach() {
        runner.attach(this)
    }

    // A reused node, such as a lazy list item showing another item, starts from rest.
    override fun onReset() {
        particles.clear()
        replaceRunner()
    }

    override fun onDetach() {
        // A fresh runner, so an animation cut off here doesn't come back frozen mid-frame on the next attach.
        runner.stop()
        runner = newRunner()
        particles.clear()
        coordinates = null
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
    }
}

/** Draws into a layer that is being recorded, with [drawContent] still drawing the element's content. */
private class LayerContentScope(
    recording: DrawScope,
    private val outer: ContentDrawScope,
) : ContentDrawScope,
    DrawScope by recording {
    override fun drawContent() = outer.drawContent()
}
