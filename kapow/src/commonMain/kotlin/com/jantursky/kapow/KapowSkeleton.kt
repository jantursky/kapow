package com.jantursky.kapow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.shimmerProgress
import com.jantursky.kapow.effects.shineBand
import com.jantursky.kapow.effects.shineStops
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

/**
 * Covers the element with a placeholder while [loading] is true, and reveals it when [loading] turns false.
 *
 * The content is always measured and placed, so its size and position are the same while loading and after: put
 * the real layout in with dummy data and it becomes the shape of the skeleton. The placeholder is a fill of [color]
 * in [shape], drawn over the content, with a band of light sweeping across it when [shimmer] is on. The reveal is
 * the placeholder fading away. An element that is first composed with [loading] false shows its content at once,
 * with no placeholder and no reveal.
 *
 * Nothing recomposes while the placeholder shimmers or fades: the animation is read only when the element is drawn.
 * The shimmer stops by itself once the placeholder is gone.
 *
 * With reduced motion the placeholder is still, with no shimmer, and the reveal is a plain cross-fade.
 *
 * ```
 * Column(Modifier.kapowSkeleton(loading)) { Text(title) }
 * ```
 *
 * The placeholder is not part of the element's semantics; mark the element as busy yourself if a screen reader
 * should hear that it is loading.
 *
 * @param loading Whether the placeholder covers the content.
 * @param shape The shape of the placeholder. Give it the shape of the content, such as a circle for an avatar, so
 * no corner of the content peeks out.
 * @param color The fill of the placeholder. With an alpha below 1 the content shows through.
 * @param shimmer Whether a band of light keeps sweeping across the placeholder.
 * @param shimmerColor The band. Its alpha is how bright the band gets at its middle.
 * @param angle The direction the band travels, in degrees clockwise from pointing right.
 * @param duration How long one sweep takes. Must be positive.
 * @param revealDuration How long the placeholder takes to fade away. Must not be negative.
 * @param revealEasing The curve of that fade.
 */
@Stable
public fun Modifier.kapowSkeleton(
    loading: Boolean,
    shape: Shape = RoundedCornerShape(8.dp),
    color: Color = Color(0xFFD9DCE3),
    shimmer: Boolean = true,
    shimmerColor: Color = Color.White.copy(alpha = 0.5f),
    angle: Float = 20f,
    duration: Duration = 1500.milliseconds,
    revealDuration: Duration = 300.milliseconds,
    revealEasing: Easing = FastOutSlowInEasing,
): Modifier {
    require(duration.isPositive()) { "duration must be positive" }
    require(!revealDuration.isNegative()) { "revealDuration must not be negative" }
    return this then
        SkeletonElement(
            loading,
            shape,
            color,
            shimmer,
            shimmerColor,
            angle,
            duration,
            revealDuration,
            revealEasing,
        )
}

private data class SkeletonElement(
    val loading: Boolean,
    val shape: Shape,
    val color: Color,
    val shimmer: Boolean,
    val shimmerColor: Color,
    val angle: Float,
    val duration: Duration,
    val revealDuration: Duration,
    val revealEasing: Easing,
) : ModifierNodeElement<SkeletonNode>() {
    override fun create(): SkeletonNode = SkeletonNode(this)

    override fun update(node: SkeletonNode) {
        node.update(this)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "kapowSkeleton"
        properties["loading"] = loading
        properties["shape"] = shape
        properties["color"] = color
        properties["shimmer"] = shimmer
        properties["shimmerColor"] = shimmerColor
        properties["angle"] = angle
        properties["duration"] = duration
        properties["revealDuration"] = revealDuration
    }
}

private class SkeletonNode(
    private var config: SkeletonElement,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    // How much of the placeholder is left: 1 while loading, 0 once revealed.
    private var cover = Animatable(if (config.loading) 1f else 0f)
    private var seconds by mutableDoubleStateOf(0.0)
    private var coverJob: Job? = null
    private var clockJob: Job? = null
    private var stops = shineStops(config.shimmerColor, peak = 1f)

    private var outline: Outline? = null
    private var outlineSize = Size.Unspecified
    private var outlineDirection: LayoutDirection? = null
    private var outlineDensity = 0f

    override val shouldAutoInvalidate: Boolean get() = false

    private fun isReduced(): Boolean =
        resolveReducedMotion(currentValueOf(LocalKapowMotion), coroutineScope.coroutineContext.durationScale)

    fun update(next: SkeletonElement) {
        val previous = config
        config = next
        if (next.shimmerColor != previous.shimmerColor) stops = shineStops(next.shimmerColor, peak = 1f)
        if (next.shape != previous.shape) outline = null
        if (next.loading != previous.loading) {
            val target = if (next.loading) 1f else 0f
            val reduced = isReduced()
            val millis = next.revealDuration.inWholeMilliseconds.toInt()
            val easing = if (reduced) LinearEasing else next.revealEasing
            coverJob?.cancel()
            coverJob = coroutineScope.launch { cover.animateTo(target, tween(millis, easing = easing)) }
        }
        startClock()
        invalidateDraw()
    }

    override fun onAttach() {
        cover = Animatable(if (config.loading) 1f else 0f)
        clockJob = null
        coverJob = null
        startClock()
    }

    // Runs while the placeholder is up or fading, and stops by itself once it is gone.
    private fun startClock() {
        if (clockJob?.isActive == true || !config.shimmer || isReduced()) return
        if (!config.loading && cover.value <= 0f) return
        clockJob =
            coroutineScope.launch {
                var last = -1L
                while (isActive) {
                    withFrameNanos { now ->
                        // A long frame moves the sweep on by at most 1/30 s.
                        if (last >= 0) seconds += ((now - last) / 1e9).coerceAtMost(MAX_STEP)
                        last = now
                    }
                    if (!config.loading && cover.value <= 0f) break
                    if (!config.shimmer) break
                }
            }
    }

    private fun outlineFor(scope: ContentDrawScope): Outline {
        val cached = outline
        if (cached != null && scope.size == outlineSize && scope.layoutDirection == outlineDirection &&
            scope.density == outlineDensity
        ) {
            return cached
        }
        val fresh = config.shape.createOutline(scope.size, scope.layoutDirection, scope)
        outline = fresh
        outlineSize = scope.size
        outlineDirection = scope.layoutDirection
        outlineDensity = scope.density
        return fresh
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha = cover.value
        if (alpha <= 0f || size.minDimension <= 0f) return
        val outline = outlineFor(this)
        drawOutline(outline, config.color, alpha)
        if (!config.shimmer || isReduced()) return
        val progress =
            shimmerProgress(seconds, config.duration.toDouble(DurationUnit.SECONDS), PAUSE_SECONDS) ?: return
        val (start, end) = shineBand(size, config.angle, progress, BAND_FRACTION)
        drawOutline(outline, Brush.linearGradient(*stops, start = start, end = end), alpha)
    }

    private companion object {
        const val MAX_STEP = 1.0 / 30
        const val PAUSE_SECONDS = 0.4
        const val BAND_FRACTION = 0.4f
    }
}
