package com.jantursky.kapow.transitions

import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.jantursky.kapow.TransitionKind
import com.jantursky.kapow.internal.EffectContext

/**
 * One direction of a transition kind that keeps its own state, such as geometry cached per size or a particle
 * watcher. [KindRunner] hands it the frames between hidden and shown; at 0 and 1 the runner draws nothing or the
 * plain content itself.
 */
internal abstract class TransitionPart {
    /** Sets the layer for a frame at [progress], strictly between 0 and 1. Masks need an offscreen layer. */
    open fun configureLayer(
        layer: GraphicsLayer,
        progress: Float,
    ) {
        layer.compositingStrategy = CompositingStrategy.Offscreen
    }

    /** Draws a frame at [progress], strictly between 0 and 1. */
    abstract fun ContentDrawScope.draw(progress: Float)

    /** Called when the runner attaches. [entering] and [progress] are read like the runner's own. */
    open fun attach(
        context: EffectContext,
        entering: () -> Boolean,
        progress: () -> Float,
    ) {}

    open fun stop() {}
}

/** The part that draws [kind], or null when [KindRunner] draws it itself. */
internal fun partFor(kind: TransitionKind): TransitionPart? =
    when (kind) {
        is TransitionKind.Dissolve -> DissolveMask(kind)
        is TransitionKind.Curtain -> CurtainMask(kind)
        is TransitionKind.ShapeIris -> ShapeIrisMask(kind)
        is TransitionKind.Checker -> CheckerMask(kind)
        is TransitionKind.Tear -> TearMask(kind)
        is TransitionKind.Blob -> BlobMask(kind)
        is TransitionKind.Shatter -> ShatterPart(kind)
        else -> null
    }
