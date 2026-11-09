package com.jantursky.kapow

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import com.jantursky.kapow.particles.LocalParticleHost
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleField
import com.jantursky.kapow.particles.ParticleHost

/**
 * Draws the particles of every Kapow effect inside [content] on top of it, so parents that clip their children
 * (lists, cards, scrolling containers) don't cut them off. Put it high in the tree, around a screen or the whole
 * app. Without one, particles draw around the element that emits them.
 *
 * Particles start at the element and move with it: if the element scrolls or moves while they are in flight, they
 * keep their place relative to it. When the element leaves the tree, or a lazy list reuses it for another item,
 * the particles stay where they were and finish there.
 */
@Composable
public fun KapowParticleLayer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val host = remember { HostState() }
    CompositionLocalProvider(LocalParticleHost provides host) {
        Box(modifier.then(ParticleLayerElement(host)), propagateMinConstraints = true) { content() }
    }
}

private class HostState : ParticleHost {
    var node: ParticleLayerNode? = null

    override fun add(
        batch: ParticleBatch,
        emitter: LayoutCoordinates,
    ): Boolean {
        val node = node ?: return false
        val coordinates = node.coordinates?.takeIf { it.isAttached && emitter.isAttached } ?: return false
        // The origin arrives in the emitter's coordinates. It is worked out again on each draw, so the particles
        // follow the emitter as it moves or scrolls, and stay where they were once it is gone.
        val local = batch.origin
        batch.origin = coordinates.localPositionOf(emitter, local)
        batch.follow = {
            val layer = node.coordinates
            if (layer != null && layer.isAttached && emitter.isAttached) layer.localPositionOf(emitter, local) else null
        }
        node.field.add(batch, node.coroutineScope)
        return true
    }
}

private class ParticleLayerElement(
    private val host: HostState,
) : ModifierNodeElement<ParticleLayerNode>() {
    override fun create(): ParticleLayerNode = ParticleLayerNode(host)

    override fun update(node: ParticleLayerNode) {
        node.host = host
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "kapowParticleLayer"
    }

    override fun equals(other: Any?): Boolean = other is ParticleLayerElement && other.host === host

    override fun hashCode(): Int = host.hashCode()
}

private class ParticleLayerNode(
    var host: HostState,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode {
    val field = ParticleField()
    var coordinates: LayoutCoordinates? = null

    override fun onAttach() {
        host.node = this
    }

    override fun onDetach() {
        if (host.node === this) host.node = null
        field.clear()
        coordinates = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        field.draw(this)
    }
}
