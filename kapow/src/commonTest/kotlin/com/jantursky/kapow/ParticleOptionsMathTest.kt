package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.confettiPiece
import com.jantursky.kapow.effects.emberEmission
import com.jantursky.kapow.effects.explosionEmission
import com.jantursky.kapow.effects.fit
import com.jantursky.kapow.effects.growIn
import com.jantursky.kapow.effects.growInFrom
import com.jantursky.kapow.effects.isRibbon
import com.jantursky.kapow.effects.riseEmission
import com.jantursky.kapow.effects.riseSpeedFor
import com.jantursky.kapow.effects.rocketEmission
import com.jantursky.kapow.effects.sparklePlacements
import com.jantursky.kapow.effects.speedLines
import com.jantursky.kapow.effects.taperedLine
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.pickColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ParticleOptionsMathTest {
    private val density = Density(1f)

    @Test
    fun sequentialColorsAreTheListAndRandomOnesFollowTheSeed() {
        val colors = listOf(Color.Red, Color.Green, Color.Blue)
        assertEquals(colors, pickColors(colors, null, 8, 1))
        assertEquals(colors, pickColors(colors, ColorPick.Sequential, 8, 1))
        val random = pickColors(colors, ColorPick.Random, 20, 7)
        assertEquals(20, random.size)
        assertEquals(random, pickColors(colors, ColorPick.Random, 20, 7))
        assertNotEquals(random, pickColors(colors, ColorPick.Random, 20, 8))
        assertTrue(random.all { it in colors })
    }

    @Test
    fun riseCoversTheAskedDistanceBeforeItExpires() {
        val seconds = 1.5f
        val speed = riseSpeedFor(80f, seconds)
        val system = ParticleSystem(riseEmission(density, speed, seconds, wobble = 0.dp), 1)
        repeat(359) { system.step(1f / 240) }
        assertEquals(-80f, system.y[0], 1.5f)
    }

    @Test
    fun theStandardRiseKeepsItsConstants() {
        val emission = riseEmission(Density(2f))
        assertEquals(140f, emission.speed.min)
        assertEquals(1.2f, emission.lifetime.min)
        assertEquals(8f, emission.wobble)
        assertEquals(-90f, emission.angle.min)
        // 70 dp per second for 1.2 seconds with drag 0.3 floats about 44 dp.
        assertEquals(44.4f, 70f / riseSpeedFor(1f, 1.2f), 0.2f)
    }

    @Test
    fun driftLeansTheRiseSideways() {
        val system = ParticleSystem(riseEmission(density, direction = -60f, wobble = 0.dp), 1)
        repeat(60) { system.step(1f / 60) }
        assertTrue(system.x[0] > 5f, "leans right: ${system.x[0]}")
    }

    @Test
    fun growInFromStartsWhereAsked() {
        for (p in listOf(0f, 0.05f, 0.15f, 0.7f)) assertEquals(growIn(p), growInFrom(0.4f, p))
        assertEquals(0f, growInFrom(0f, 0f))
        assertEquals(1f, growInFrom(0f, 0.2f))
    }

    @Test
    fun ribbonsFollowTheRatio() {
        val third = (0 until 9).filter { isRibbon(it, 1f / 3f) }
        assertEquals(listOf(2, 5, 8), third)
        assertEquals(0, (0 until 30).count { isRibbon(it, 0f) })
        assertEquals(30, (0 until 30).count { isRibbon(it, 1f) })
        assertEquals(15, (0 until 30).count { isRibbon(it, 0.5f) })
        assertEquals(confettiPiece(2, 7f, 1f / 3f), confettiPiece(2, 7f))
        assertEquals(3.5f, confettiPiece(2, 7f).width, 1e-4f)
        assertEquals(15.4f, confettiPiece(2, 7f).height, 1e-4f)
        assertEquals(9.8f, confettiPiece(1, 7f).height, 1e-4f)
    }

    @Test
    fun theRocketClimbsItsHeightInTheAskedTime() {
        val system = ParticleSystem(rocketEmission(height = 120f, seconds = 0.9f), seed = 1)
        repeat(107) { system.step(1f / 120) }
        assertEquals(-120f, system.y[0], 8f)
        repeat(30) { system.step(1f / 120) }
        assertTrue(!system.isAlive(0))
    }

    @Test
    fun trailLengthScalesEmbersAndReachScalesTheBurst() {
        assertEquals(emberEmission(density), emberEmission(density, 1f))
        assertEquals(2f, emberEmission(density, 2f).lifetime.min / emberEmission(density).lifetime.min)
        assertEquals(explosionEmission(10, density), explosionEmission(10, density, 1f))
        assertEquals(
            explosionEmission(10, density).speed.max * 2,
            explosionEmission(10, density, 2f).speed.max,
        )
    }

    @Test
    fun speedLineDirectionsKeepTheLinesInTheirCones() {
        val half = PI.toFloat() / 2
        val limit = cos(40f * PI.toFloat() / 180f)

        fun near(
            angle: Float,
            vararg targets: Float,
        ) = targets.any { cos(angle - it) > limit }
        val horizontal = speedLines(24, 3, SpeedLineDirections.Horizontal)
        val vertical = speedLines(24, 3, SpeedLineDirections.Vertical)
        val arc = speedLines(24, 3, SpeedLineDirections.Arc)
        for (i in 0 until 24) {
            assertTrue(near(horizontal[3 * i], 0f, 2 * half), "horizontal ${horizontal[3 * i]}")
            assertTrue(near(vertical[3 * i], half, 3 * half), "vertical ${vertical[3 * i]}")
            assertTrue(sin(arc[3 * i]) <= 1e-4f, "arc ${arc[3 * i]}")
        }
        assertTrue(abs(cos(horizontal[0])) > 0.7f)
        assertTrue(speedLines(12, 3).contentEquals(speedLines(12, 3, SpeedLineDirections.All)))
        assertEquals(1.2f, SpeedLineDirections.All.fit(1.2f))
    }

    @Test
    fun aTaperedLineNarrowsToItsHead() {
        val flat = taperedLine(Offset(0f, 0f), Offset(10f, 0f), 4f, 0f)
        val pointed = taperedLine(Offset(0f, 0f), Offset(10f, 0f), 4f, 1f)
        assertEquals(4f, flat.getBounds().height)
        assertEquals(4f, pointed.getBounds().height)
        assertEquals(10f, pointed.getBounds().right)
        assertEquals(4f, flat.getBounds().height)
    }

    @Test
    fun sparkleSpreadScalesTheAreaAroundItsCenter() {
        val size = Size(100f, 100f)
        val wide = sparklePlacements(12, size, 2, spread = 2f)
        val narrow = sparklePlacements(12, size, 2, spread = 0.5f)
        assertTrue((0 until 12).any { wide[2 * it] < 0f || wide[2 * it] > 100f }, "some outside")
        assertTrue((0 until 12).all { narrow[2 * it] in 25f..75f && narrow[2 * it + 1] in 25f..75f })
        assertTrue(sparklePlacements(12, size, 2).contentEquals(sparklePlacements(12, size, 2, 1f)))
    }
}
