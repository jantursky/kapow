package com.jantursky.kapow.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.LocalKapowFeedback
import com.jantursky.kapow.KapowFeedbackEvent
import com.jantursky.kapow.LocalKapowMotion
import com.jantursky.kapow.durationScale
import com.jantursky.kapow.particles.LocalParticleHost
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleField
import com.jantursky.kapow.resolveReducedMotion
import kotlinx.coroutines.CoroutineScope

/**
 * The node behind every Kapow modifier. It hosts one [EffectRunner], gives it a graphics layer when it asks for
 * one, draws it, and draws the runner's particles itself when no particle layer is above the element.
 */
internal abstract class EffectNode<R : EffectRunner>(
    runner: R,
) : Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode,
    EffectContext {
    var runner: R = runner
        private set

    private val particles = ParticleField()
    private var coordinates: LayoutCoordinates? = null

    override val shouldAutoInvalidate: Boolean get() = false

    final override var size: IntSize = IntSize.Zero
        private set

    override val scope: CoroutineScope get() = coroutineScope
    override val density: Density get() = requireDensity()
    override val layoutDirection: LayoutDirection get() = requireLayoutDirection()
    override val fontFamilyResolver: FontFamily.Resolver get() = currentValueOf(LocalFontFamilyResolver)

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

    /** Swaps in a new runner, for when the effect's configuration changes. */
    fun replaceRunner(runner: R) {
        this.runner.stop()
        this.runner = runner
        if (isAttached) {
            invalidatePlacement()
            invalidateDraw()
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        size = IntSize(placeable.width, placeable.height)
        return layout(placeable.width, placeable.height) {
            val runner = runner
            if (runner.needsLayer) {
                placeable.placeWithLayer(0, 0) { with(runner) { applyLayer() } }
            } else {
                placeable.place(0, 0)
            }
        }
    }

    override fun ContentDrawScope.draw() {
        with(runner) { drawEffect() }
        particles.draw(this)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun onDetach() {
        runner.stop()
        particles.clear()
        coordinates = null
    }
}
