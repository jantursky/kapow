package com.jantursky.kapow

import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.internal.applyTo
import com.jantursky.kapow.internal.growthCurve
import com.jantursky.kapow.internal.offsets
import com.jantursky.kapow.internal.opacityCurve
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.Spread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParticlePhysicsTest {
    private val base =
        Emission(
            count = 5,
            angle = Spread(-120f, -60f),
            speed = Spread(100f, 200f),
            lifetime = Spread(1f),
            gravity = 50f,
            drag = 0.5f,
            scale = Spread(1f, 2f),
        )
    private val area = Size(100f, 40f)

    @Test
    fun theDefaultChangesNothing() {
        assertEquals(base, ParticlePhysics.Default.applyTo(base, density = 3f, area = area))
    }

    @Test
    fun equalPhysicsAreEqual() {
        assertEquals(ParticlePhysics.of(gravity = 5f, speed = 1f..2f), ParticlePhysics.of(gravity = 5f, speed = 1f..2f))
    }

    @Test
    fun directionAndSpreadSetTheLaunchCone() {
        val cone = ParticlePhysics.of(direction = 0f, spread = 90f).applyTo(base, 1f, area)
        assertEquals(Spread(-45f, 45f), cone.angle)
        val keepsWidth = ParticlePhysics.of(direction = 0f).applyTo(base, 1f, area)
        assertEquals(Spread(-30f, 30f), keepsWidth.angle, "the cone keeps its width when only the direction is set")
    }

    @Test
    fun dpValuesBecomePixels() {
        val tuned = ParticlePhysics.of(speed = 10f..20f, gravity = 100f).applyTo(base, density = 2f, area = area)
        assertEquals(Spread(20f, 40f), tuned.speed)
        assertEquals(200f, tuned.gravity)
    }

    @Test
    fun sizeScalesTheEffectsOwnSizes() {
        val tuned = ParticlePhysics.of(size = 0.5f..2f).applyTo(base, 1f, area)
        assertEquals(Spread(0.5f, 4f), tuned.scale)
    }

    @Test
    fun emissionAreasCenterOnTheOrigin() {
        assertEquals(Spread(-50f, 50f) to Spread(-20f), EmissionArea.Edge(EdgeSide.Top).offsetsFor(area))
        assertEquals(Spread(-50f, 50f) to Spread(20f), EmissionArea.Edge(EdgeSide.Bottom).offsetsFor(area))
        assertEquals(Spread(-50f, 50f) to Spread(-20f, 20f), EmissionArea.Rect.offsetsFor(area))
        assertEquals(Spread(0f) to Spread(0f), EmissionArea.Point.offsetsFor(area))
    }

    @Test
    fun startAndEndEdgesFollowTheLayoutDirection() {
        val start = EmissionArea.Edge(EdgeSide.Start)
        assertEquals(Spread(-50f), start.offsetsFor(area, LayoutDirection.Ltr).first)
        assertEquals(Spread(50f), start.offsetsFor(area, LayoutDirection.Rtl).first)
    }

    @Test
    fun fadeAndScaleCurvesFallBackToTheEffectsOwn() {
        val own: (Float) -> Float = { 0.25f }
        assertEquals(0.25f, ParticlePhysics.Default.opacityCurve(own)(0.5f))
        assertEquals(0.25f, ParticlePhysics.Default.growthCurve(own)(0.5f))
        val fade = ParticlePhysics.of(fade = LinearEasing).opacityCurve(own)
        assertEquals(0.5f, fade(0.5f), "opacity is what the fade easing leaves")
        val grow = ParticlePhysics.of(startScale = 0f, endScale = 2f).growthCurve(own)
        assertEquals(1f, grow(0.5f))
    }

    @Test
    fun invalidPartsAreRejected() {
        assertFailsWith<IllegalArgumentException> { ParticlePhysics.of(spread = 400f) }
        assertFailsWith<IllegalArgumentException> { ParticlePhysics.of(drag = 0f) }
        assertFailsWith<IllegalArgumentException> { ParticlePhysics.of(lifetime = 0f..1f) }
        assertFailsWith<IllegalArgumentException> { ParticlePhysics.of(speed = -1f..1f) }
        assertFailsWith<IllegalArgumentException> { ParticlePhysics.of(endScale = -1f) }
    }

    private fun EmissionArea.offsetsFor(
        area: Size,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) = offsets(area, direction)
}
