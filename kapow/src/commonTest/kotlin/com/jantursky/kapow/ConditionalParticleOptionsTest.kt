package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.bubbleGrowth
import com.jantursky.kapow.effects.bubbleOpacity
import com.jantursky.kapow.effects.bubbleVariants
import com.jantursky.kapow.effects.flameEmission
import com.jantursky.kapow.effects.orbitScale
import com.jantursky.kapow.effects.puffOpacity
import com.jantursky.kapow.effects.smokeEmission
import com.jantursky.kapow.effects.sparkleEmission
import com.jantursky.kapow.effects.sparkleInterval
import com.jantursky.kapow.internal.atSpeed
import com.jantursky.kapow.internal.scaledInterval
import com.jantursky.kapow.internal.scatter
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ConditionalParticleOptionsTest {
    private val density = Density(1f)

    @Test
    fun smokeOpacityPeaksAtTheChosenValue() {
        assertEquals(0.3f, puffOpacity(0.2f, 0.3f))
        assertEquals(0.7f, puffOpacity(0.2f))
    }

    @Test
    fun windPushesPuffsSideways() {
        val calm = ParticleSystem(smokeEmission(100f, density), 4)
        val windy = ParticleSystem(smokeEmission(100f, density, wind = 80f), 4)
        calm.step(1f)
        windy.step(1f)
        assertTrue(windy.x[0] > calm.x[0] + 20f)
    }

    @Test
    fun bubblesCanThinOutInsteadOfPopping() {
        assertTrue(bubbleOpacity(0.8f, pop = false) < 1f, "already thinning at 0.8")
        assertEquals(1f, bubbleOpacity(0.8f, pop = true))
        assertTrue(bubbleGrowth(1f, pop = false) < bubbleGrowth(1f, pop = true))
    }

    @Test
    fun colorVariationGivesAFewTints() {
        assertEquals(1, bubbleVariants(Color.Blue, 5f, 0f).size)
        assertTrue(bubbleVariants(Color.Blue, 5f, 0.5f).size > 1)
    }

    @Test
    fun aFlameLeavesTheEdgeItBurnsFrom() {
        fun launch(edge: EdgeSide): Emission = flameEmission(100f, 40f, edge)
        assertEquals(Spread(-96f, -84f), launch(EdgeSide.Top).angle)
        assertEquals(Spread(84f, 96f), launch(EdgeSide.Bottom).angle)
        assertEquals(Spread(174f, 186f), launch(EdgeSide.Start).angle)
        assertEquals(Spread(-6f, 6f), launch(EdgeSide.End).angle)
        // In a right to left layout the start edge is on the right.
        assertEquals(Spread(-6f, 6f), flameEmission(100f, 40f, EdgeSide.Start, rtl = true).angle)
    }

    @Test
    fun theFlameBaseWidthAndFlickerFollowTheirOptions() {
        val wide = flameEmission(100f, 40f, flameWidth = 60f)
        assertEquals(Spread(-30f, 30f), wide.offsetX)
        assertEquals(6f, flameEmission(100f, 40f, flicker = 2f).wobbleRate)
        assertEquals(3f, flameEmission(100f, 40f).wobbleRate)
    }

    @Test
    fun sparklesAppearWithinThePadding() {
        repeat(40) { seed ->
            val inside = ParticleSystem(sparkleEmission(100f, 40f, 20f, padding = -5f), seed)
            assertTrue(kotlin.math.abs(inside.x[0]) <= 45f && kotlin.math.abs(inside.y[0]) <= 15f)
        }
    }

    @Test
    fun sparkleLifetimeSetsHowOftenTheyStart() {
        assertTrue(sparkleInterval(4, 2f..3f) > sparkleInterval(4, 0.5f..0.6f))
        assertEquals(sparkleInterval(4), sparkleInterval(4, 0.8f..1.3f))
    }

    @Test
    fun theSameSeedGivesTheSamePuffs() {
        val a = ParticleSystem(sparkleEmission(100f, 40f, 20f), 9)
        val b = ParticleSystem(sparkleEmission(100f, 40f, 20f), 9)
        val c = ParticleSystem(sparkleEmission(100f, 40f, 20f), 10)
        assertEquals(a.x[0], b.x[0])
        assertEquals(a.y[0], b.y[0])
        assertNotEquals(a.x[0], c.x[0])
    }

    @Test
    fun orbitParticlesShrinkToTheDepthScaleAtTheBack() {
        assertEquals(1f, orbitScale(1f, 0.3f))
        assertEquals(0.3f, orbitScale(-1f, 0.3f))
        assertEquals(0.55f, orbitScale(-1f))
    }

    @Test
    fun speedPlaysTheSamePathInLessTime() {
        val base = Emission(1, Spread(-90f), Spread(100f), Spread(2f), gravity = 50f, wobbleRate = 1f)
        val fast = base.atSpeed(2f)
        val slow = ParticleSystem(base, 1)
        val quick = ParticleSystem(fast, 1)
        slow.step(1f)
        quick.step(0.5f)
        assertTrue(kotlin.math.abs(slow.y[0] - quick.y[0]) < 0.5f)
        assertEquals(1f, fast.lifetime.min)
        assertEquals(base, base.atSpeed(1f))
    }

    @Test
    fun theIntervalShrinksWithTheRate() {
        assertEquals(70.milliseconds, scaledInterval(140.milliseconds, 2f))
        assertEquals(140.milliseconds, scaledInterval(140.milliseconds, 1f))
    }

    @Test
    fun scatterIsReproducibleAndInRange() {
        assertEquals(scatter(3, 7, 8), scatter(3, 7, 8))
        repeat(100) { assertTrue(scatter(it, it * 3, 8) in 0 until 8) }
        assertTrue((0 until 50).map { scatter(1, it, 8) }.toSet().size > 3)
    }

    @Test
    fun equalOptionsGiveEqualEffects() {
        val physics = ParticlePhysics.of(gravity = 40f)
        assertEquals(
            ConditionalEffect.smoke(rate = 2f, physics = physics, seed = 3),
            ConditionalEffect.smoke(rate = 2f, physics = ParticlePhysics.of(gravity = 40f), seed = 3),
        )
        assertNotEquals(ConditionalEffect.smoke(rate = 2f), ConditionalEffect.smoke(rate = 3f))
        assertEquals(ConditionalEffect.orbit(), ConditionalEffect.orbit())
    }

    @Test
    fun theFactoriesCheckTheNewOptions() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.smoke(rate = 0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.smoke(size = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.smoke(opacity = 1.5f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.bubbles(rate = -1f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.bubbles(wobble = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.bubbles(colorVariation = 2f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.flame(width = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.flame(intensity = 0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.flame(flicker = -1f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.sparkles(lifetime = 0f..1f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.orbit(radius = 0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.orbit(depthScale = 0f) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.orbit(trail = -1) }
    }
}
