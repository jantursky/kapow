package com.jantursky.kapow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.tilt.TiltAngles
import com.jantursky.kapow.tilt.sheenCenter
import com.jantursky.kapow.tilt.tiltAngles
import com.jantursky.kapow.tilt.tiltStrength
import kotlinx.coroutines.launch

/**
 * Tilts the element in 3D toward a hovering mouse or a pressed finger, by at most [maxAngle] degrees, and springs it
 * back to flat when the pointer leaves or lifts. The side of the element the pointer is on dips away, in proportion
 * to how far the pointer is from the middle.
 *
 * The modifier only watches pointer events and never consumes them, so clicks, drags and scrolls reach the content
 * as if it were not there. Nothing recomposes while it tilts: the angles are read only when the layer is drawn.
 *
 * With [enabled] false, or with reduced motion, the element stays flat and draws no sheen.
 *
 * ```
 * Card(Modifier.kapowTilt(maxAngle = 10f)) { ... }
 * ```
 *
 * @param maxAngle The angle in degrees at the edge of the element. Must not be negative.
 * @param sheen The color of a soft highlight that moves across the element opposite to the tilt, stronger the more it
 * tilts. Null draws none.
 * @param enabled Whether the element tilts.
 * @param perspective The camera distance of the layer, the same unit as the platform's `cameraDistance`. The
 * default is the platform default. A smaller distance makes the perspective stronger, and a large element looks
 * more natural with a larger one. Must be positive.
 * @param damping The damping ratio of the springs that follow the pointer and bring the element back. Lower
 * values overshoot more. Must be positive.
 */
@Stable
public fun Modifier.kapowTilt(
    maxAngle: Float = 12f,
    sheen: Color? = Color.White.copy(alpha = 0.35f),
    enabled: Boolean = true,
    perspective: Dp = 8.dp,
    damping: Float = 0.6f,
): Modifier {
    require(maxAngle >= 0f && maxAngle.isFinite()) { "maxAngle must not be negative" }
    require(perspective.value > 0f && perspective.value.isFinite()) { "perspective must be positive" }
    require(damping > 0f && damping.isFinite()) { "damping must be positive" }
    return this then TiltElement(maxAngle, sheen, enabled, perspective, damping)
}

private data class TiltElement(
    val maxAngle: Float,
    val sheen: Color?,
    val enabled: Boolean,
    val perspective: Dp,
    val damping: Float,
) : ModifierNodeElement<TiltNode>() {
    override fun create(): TiltNode = TiltNode(maxAngle, sheen, enabled, perspective, damping)

    override fun update(node: TiltNode) {
        node.update(maxAngle, sheen, enabled, perspective, damping)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "kapowTilt"
        properties["maxAngle"] = maxAngle
        properties["sheen"] = sheen
        properties["enabled"] = enabled
        properties["perspective"] = perspective
        properties["damping"] = damping
    }
}

private class TiltNode(
    private var maxAngle: Float,
    private var sheen: Color?,
    private var enabled: Boolean,
    private var perspective: Dp,
    private var damping: Float,
) : Modifier.Node(),
    PointerInputModifierNode,
    LayoutModifierNode,
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    private val rotationX = Animatable(0f)
    private val rotationY = Animatable(0f)

    fun update(
        maxAngle: Float,
        sheen: Color?,
        enabled: Boolean,
        perspective: Dp,
        damping: Float,
    ) {
        this.maxAngle = maxAngle
        this.sheen = sheen
        this.perspective = perspective
        this.damping = damping
        if (enabled != this.enabled) {
            this.enabled = enabled
            if (!enabled) goFlat()
        }
    }

    private val active: Boolean
        get() =
            enabled &&
                !resolveReducedMotion(currentValueOf(LocalKapowMotion), coroutineScope.coroutineContext.durationScale)

    private fun angles(): TiltAngles = TiltAngles(rotationX.value, rotationY.value)

    private fun springTo(target: TiltAngles) {
        val spec = spring<Float>(dampingRatio = damping, stiffness = Spring.StiffnessMediumLow)
        coroutineScope.launch { rotationX.animateTo(target.rotationX, spec) }
        coroutineScope.launch { rotationY.animateTo(target.rotationY, spec) }
    }

    private fun goFlat() {
        if (rotationX.targetValue == 0f && rotationY.targetValue == 0f && rotationX.value == 0f &&
            rotationY.value == 0f
        ) {
            return
        }
        coroutineScope.launch {
            rotationX.snapTo(0f)
            rotationY.snapTo(0f)
        }
    }

    // The sibling content below the element gets the same events.
    override fun sharePointerInputWithSiblings(): Boolean = true

    override fun onCancelPointerInput() {
        springTo(TiltAngles.Flat)
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        // Watching in the first pass and consuming nothing leaves every event for the content below.
        if (pass != PointerEventPass.Initial || !active) return
        val change = pointerEvent.changes.firstOrNull() ?: return
        val mouse = change.type == PointerType.Mouse
        when (pointerEvent.type) {
            PointerEventType.Press, PointerEventType.Move, PointerEventType.Enter -> {
                if (mouse || change.pressed) follow(change.position, bounds)
            }

            // A mouse button going up leaves the pointer hovering; a finger going up ends the tilt.
            PointerEventType.Release -> {
                if (mouse) follow(change.position, bounds) else springTo(TiltAngles.Flat)
            }

            PointerEventType.Exit -> {
                springTo(TiltAngles.Flat)
            }
        }
    }

    private fun follow(
        position: Offset,
        bounds: IntSize,
    ) {
        springTo(tiltAngles(position, Size(bounds.width.toFloat(), bounds.height.toFloat()), maxAngle))
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                // Read here, so the animation only redraws the layer.
                if (enabled) {
                    rotationX = this@TiltNode.rotationX.value
                    rotationY = this@TiltNode.rotationY.value
                    cameraDistance = this@TiltNode.perspective.toPx()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val color = sheen ?: return
        if (!enabled) return
        val angles = angles()
        val strength = tiltStrength(angles, maxAngle)
        if (strength <= 0f) return
        val center = sheenCenter(angles, maxAngle, size)
        val radius = maxOf(size.width, size.height) * 0.8f
        drawRect(
            Brush.radialGradient(
                colors = listOf(color.copy(alpha = color.alpha * strength), Color.Transparent),
                center = center,
                radius = radius,
            ),
        )
    }
}
