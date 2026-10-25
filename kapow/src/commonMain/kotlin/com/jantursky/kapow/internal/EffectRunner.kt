package com.jantursky.kapow.internal

import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.ReducedMotionMode
import com.jantursky.kapow.particles.ParticleBatch
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration

/**
 * The part of an effect that runs on an element. It keeps its animation state in snapshot state and reads it only
 * in [drawEffect] and [configureLayer], so animation frames redraw the element without recomposing anything.
 */
internal abstract class EffectRunner {
    /**
     * Whether [drawEffect] draws into a graphics layer of its own, set up by [configureLayer]. Effects that blend
     * with the element's pixels or transform them need one.
     */
    open val needsLayer: Boolean get() = false

    /** Sets the layer's transform and compositing for this frame. Called in the draw phase, before [drawEffect]. */
    open fun DrawScope.configureLayer(layer: GraphicsLayer) {}

    open fun ContentDrawScope.drawEffect() {
        drawContent()
    }

    /** Called when the node attaches, or when this runner replaces another on an attached node. */
    open fun attach(context: EffectContext) {}

    /** Stops running work. Called when the element leaves the tree or the effect is replaced. */
    open fun stop() {}
}

/** What a runner can reach on the element it is attached to. */
internal interface EffectContext {
    val scope: CoroutineScope
    val density: Density
    val layoutDirection: LayoutDirection
    val fontFamilyResolver: FontFamily.Resolver
    val graphicsContext: GraphicsContext

    /** The element's size. Zero until it is measured. */
    val size: IntSize

    /** Reads the reduced-motion setting now, as the effect fires. */
    fun isReducedMotion(): Boolean

    /** A factor on time, set by `speed(...)`: 2 plays twice as fast. Runners divide their durations by it. */
    val speed: Float get() = 1f

    /** What to do under reduced motion, set by `reducedMotion(...)`. Runners honor [ReducedMotionMode.EndState] and [ReducedMotionMode.Fade]. */
    val reducedMotionMode: ReducedMotionMode get() = ReducedMotionMode.Default

    /** How long a conditional effect fades in, when `fadeIn(...)` set it; null leaves the effect's own. */
    val fadeIn: Duration? get() = null

    /** How long a conditional effect fades out, when `fadeOut(...)` set it; null leaves the effect's own. */
    val fadeOut: Duration? get() = null

    /** Hands particles to the nearest particle layer, or draws them around the element without one. */
    fun emit(batch: ParticleBatch)

    /** The element as last drawn by a runner that [EffectRunner.needsLayer], or null without a layer. */
    suspend fun snapshot(): ImageBitmap? = null
}

/** A runner that plays once each time its effect fires. */
internal abstract class ChangeRunner : EffectRunner() {
    abstract fun fire(context: EffectContext)
}

/** A runner that is on while a condition holds. */
internal abstract class ConditionalRunner : EffectRunner() {
    abstract fun setActive(
        context: EffectContext,
        active: Boolean,
    )
}

/** A runner drawn at a progress the caller controls, from 0 to 1. */
internal abstract class ProgressRunner : EffectRunner() {
    /** Read in the draw phase, so a progress backed by snapshot state redraws without recomposition. */
    lateinit var progress: () -> Float
}

/**
 * A runner for one direction of a visibility transition. [progress] goes from 0 (hidden) to 1 (shown): up while
 * entering, down while exiting. Both lambdas are read in the draw and layer phases.
 */
internal abstract class TransitionRunner : EffectRunner() {
    lateinit var progress: () -> Float
    lateinit var entering: () -> Boolean
}
