package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** Pixels outside the element's box (150 to 250) that aren't white, and their mean x. */
private fun ImageBitmap.outside(region: (x: Int, y: Int) -> Boolean = { _, _ -> true }): Pair<Int, Float> {
    val pixels = toPixelMap()
    var n = 0
    var sum = 0L
    for (y in 0 until height) {
        for (x in 0 until width) {
            val inside = x in 150 until 250 && y in 150 until 250
            if (!inside && region(x, y) && pixels[x, y] != Color.White) {
                n++
                sum += x
            }
        }
    }
    return n to if (n == 0) 0f else sum.toFloat() / n
}

@OptIn(ExperimentalTestApi::class)
class ConditionalParticleOptionsFrameTest {
    private fun frameOf(
        effect: ConditionalEffect,
        millis: Long = 800,
    ): ImageBitmap {
        var image: ImageBitmap? = null
        val scene = LoopScene()
        runEffectTest {
            scene.show(this, effect)
            scene.on = true
            mainClock.advanceTimeBy(millis)
            image = capture()
        }
        return image!!
    }

    @Test
    fun windBlowsSmokeToTheSide() {
        val calm = frameOf(ConditionalEffect.smoke()).outside()
        val windy = frameOf(ConditionalEffect.smoke(wind = 300f)).outside()
        assertTrue(calm.first > 0)
        assertTrue(windy.second > calm.second + 5f, "mean x ${windy.second} vs ${calm.second}")
    }

    @Test
    fun smokeCanComeOutOfAnotherEdge() {
        val physics =
            ParticlePhysics.of(direction = 90f, gravity = 80f, emission = EmissionArea.Edge(EdgeSide.Bottom))
        val below = frameOf(ConditionalEffect.smoke(physics = physics)).outside { _, y -> y >= 250 }
        assertTrue(below.first > 0, "smoke below the element")
    }

    @Test
    fun aHigherRateMakesMorePuffs() {
        val few = frameOf(ConditionalEffect.smoke(rate = 0.4f)).outside().first
        val many = frameOf(ConditionalEffect.smoke(rate = 3f)).outside().first
        assertTrue(many > few, "$many vs $few")
    }

    @Test
    fun theSameSeedGivesTheSameFrame() {
        val a = frameOf(ConditionalEffect.sparkles(seed = 5))
        val b = frameOf(ConditionalEffect.sparkles(seed = 5))
        val c = frameOf(ConditionalEffect.sparkles(seed = 6))
        assertEquals(0, a.pixelsDifferentFrom(b))
        assertTrue(a.pixelsDifferentFrom(c) > 0)
    }

    @Test
    fun aFlameCanBurnFromTheBottomEdge() {
        val image = frameOf(ConditionalEffect.flame(edge = EdgeSide.Bottom))
        assertTrue(image.outside { _, y -> y >= 250 }.first > 0, "flames below")
        assertEquals(0, image.nonWhiteAbove())
    }

    @Test
    fun aFlameCanBurnFromTheSide() {
        val image = frameOf(ConditionalEffect.flame(edge = EdgeSide.End))
        assertTrue(image.outside { x, _ -> x >= 250 }.first > 0, "flames to the right")
    }

    @Test
    fun denserFlamesDrawMorePixels() {
        val thin = frameOf(ConditionalEffect.flame(intensity = 0.3f)).outside().first
        val dense = frameOf(ConditionalEffect.flame(intensity = 2f)).outside().first
        assertTrue(dense > thin, "$dense vs $thin")
    }

    @Test
    fun bubblesDrawMoreOftenAtAHigherRate() {
        val few = frameOf(ConditionalEffect.bubbles(rate = 0.3f)).outside().first
        val many = frameOf(ConditionalEffect.bubbles(rate = 3f)).outside().first
        assertTrue(many > few, "$many vs $few")
    }

    @Test
    fun orbitTurnsEitherWay() {
        val clockwise = frameOf(ConditionalEffect.orbit(listOf(Color.Red)), 700)
        val back = frameOf(ConditionalEffect.orbit(listOf(Color.Red), clockwise = false), 700)
        assertTrue(clockwise.pixelsDifferentFrom(back) > 0)
    }

    @Test
    fun orbitCanCarryAShapeInsteadOfDots() {
        val dots = frameOf(ConditionalEffect.orbit(listOf(Color.Red)), 700)
        val shape = ParticleContent.shape(ParticleShape.Square, listOf(Color.Red), 12.dp)
        val squares = frameOf(ConditionalEffect.orbit(listOf(Color.Red), content = shape), 700)
        assertTrue(squares.outside().first > 0)
        assertTrue(dots.pixelsDifferentFrom(squares) > 0)
    }

    @Test
    fun aLongerFadeInStartsMoreSlowly() {
        val quick = frameOf(ConditionalEffect.orbit(listOf(Color.Red)), 200).outside().first
        val slow = frameOf(ConditionalEffect.orbit(listOf(Color.Red)).fadeIn(3.seconds), 200).outside().first
        assertTrue(slow < quick, "$slow vs $quick")
    }

    @Test
    fun aLongerFadeOutLingers() {
        val scene = LoopScene()
        runEffectTest {
            scene.show(this, ConditionalEffect.orbit(listOf(Color.Red)).fadeOut(3.seconds))
            scene.on = true
            mainClock.advanceTimeBy(800)
            scene.on = false
            mainClock.advanceTimeBy(600)
            assertTrue(capture().outside().first > 0, "still fading after the usual 0.3 s")
        }
    }

    @Test
    fun speedTurnsTheOrbitFaster() {
        val normal = frameOf(ConditionalEffect.orbit(listOf(Color.Red)), 900)
        val fast = frameOf(ConditionalEffect.orbit(listOf(Color.Red)).speed(2f), 900)
        assertTrue(normal.pixelsDifferentFrom(fast) > 0)
    }
}
