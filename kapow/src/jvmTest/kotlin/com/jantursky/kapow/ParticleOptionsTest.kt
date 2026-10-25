package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.RiseRunner
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class ParticleOptionsTest {
    private var value by mutableIntStateOf(0)
    private val squares = ParticleContent.shape(ParticleShape.Square, listOf(Color.Red), 8.dp)

    private fun ComposeUiTest.scene(
        effect: ChangeEffect,
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            KapowParticleLayer(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, effect))
                }
            }
        }
    }

    private fun Color.isReddish() = red > 0.6f && green < 0.6f && blue < 0.6f

    /** Reddish pixels in the window area x in [xs] and y in [ys], where only particles can be outside the element. */
    private fun ImageBitmap.red(
        xs: IntRange = 0 until 400,
        ys: IntRange = 0 until 400,
    ): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in ys) for (x in xs) if (pixels[x, y].isReddish()) n++
        return n
    }

    private fun ImageBitmap.pixels(): List<Color> {
        val map = toPixelMap()
        return List(400 * 400) { map[it % 400, it / 400] }
    }

    @Test
    fun sprayPhysicsSendsParticlesTheWayItSays() =
        runEffectTest {
            val right = ParticlePhysics.of(direction = 0f, spread = 0f, gravity = 0f, drag = 1f, speed = 300f..300f)
            scene(ChangeEffect.spray(squares, 12, right))
            value++
            mainClock.advanceTimeBy(250)
            val image = capture()
            assertTrue(image.red(xs = 262 until 400) > 0, "particles to the right")
            assertEquals(0, image.red(ys = 0 until 140), "none above")
            assertEquals(0, image.red(xs = 0 until 138), "none to the left")
        }

    @Test
    fun sprayStartsAtTheOrigin() =
        runEffectTest {
            val still = ParticlePhysics.of(speed = 0f..0f, gravity = 0f, lifetime = 1f..1f, spread = 0f)
            scene(ChangeEffect.spray(squares, 6, still, origin = TransformOrigin(0f, 0f)))
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertTrue(image.red(xs = 140 until 165, ys = 140 until 165) > 0, "at the top left corner")
            assertEquals(0, image.red(xs = 190 until 400, ys = 190 until 400), "not at the center")
        }

    @Test
    fun theSameSeedGivesTheSameFramesAndAnotherSeedDoesNot() {
        fun frame(seed: Int): List<Color> {
            var pixels = emptyList<Color>()
            runEffectTest {
                scene(ChangeEffect.spray(squares, 14, seed = seed))
                value++
                mainClock.advanceTimeBy(300)
                pixels = capture().pixels()
            }
            return pixels
        }
        assertEquals(frame(4), frame(4))
        assertNotEquals(frame(4), frame(5))
    }

    @Test
    fun randomColorPickUsesEveryColorEventually() =
        runEffectTest {
            val content = ParticleContent.shape(ParticleShape.Square, listOf(Color.Red, Color.Green), 10.dp)
            val pick = ParticlePhysics.of(colorPick = ColorPick.Random)
            scene(ChangeEffect.spray(content, 20, pick, seed = 3))
            value++
            mainClock.advanceTimeBy(250)
            val colors = capture().pixels().toSet()
            assertTrue(colors.any { it.red > 0.9f && it.green < 0.1f }, "some red")
            assertTrue(colors.any { it.green > 0.4f && it.red < 0.1f }, "some green")
        }

    @Test
    fun anAdditiveBlendBrightensWhatIsBehind() =
        runEffectTest {
            val still =
                ParticlePhysics.of(
                    speed = 0f..0f,
                    gravity = 0f,
                    lifetime = 1f..1f,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Plus,
                )
            val dim = ParticleContent.shape(ParticleShape.Square, listOf(Color(0xFF200000)), 20.dp)
            scene(ChangeEffect.spray(dim, 1, still, rotation = 0f..0f))
            value++
            mainClock.advanceTimeBy(100)
            // Plus on the cobalt element lifts the red channel by the particle's.
            assertTrue(capture().colorAt(200, 200).red > Cobalt.red + 0.05f)
        }

    @Test
    fun riseShowsTheLabelOfTheChangedValue() {
        val runner = RiseRunner(ParticleContent.text("+1"), label = { "n=$it" })
        val content = runner.content(7)
        assertEquals("n=7", (content as ParticleContent.Text).text)
        val image = RiseRunner(ParticleContent.shape(), label = { "n=$it" }).content(7)
        assertTrue(image is ParticleContent.Shape, "only text is relabelled")
    }

    @Test
    fun riseFloatsAsHighAsAsked() =
        runEffectTest {
            val far = ChangeEffect.rise(squares, distance = 100.dp, duration = 1500.milliseconds, wobble = 0.dp)
            scene(far)
            value++
            mainClock.advanceTimeBy(1000)
            assertTrue(capture().red(ys = 0 until 100) > 0, "reaches 100 dp above the top edge")
        }

    @Test
    fun riseDriftsSideways() =
        runEffectTest {
            scene(ChangeEffect.rise(squares, drift = 70f, wobble = 0.dp))
            value++
            mainClock.advanceTimeBy(900)
            assertTrue(capture().red(xs = 215 until 400) > 0, "leaned to the right of the middle")
        }

    @Test
    fun fireworksOptionsMoveTheBurst() =
        runEffectTest {
            val low = ChangeEffect.fireworks(listOf(Color.Red), 20, rocketHeight = 60.dp, burstRadius = 40.dp)
            scene(low)
            value++
            mainClock.advanceTimeBy(1000)
            assertEquals(0, capture().red(ys = 0 until 40), "a small burst low down")
        }

    @Test
    fun fireworksWithoutATrailStillBurstsAfterTheClimb() =
        runEffectTest {
            val effect = ChangeEffect.fireworks(listOf(Color.Red), 20, trailLength = 0f)
            scene(effect)
            value++
            mainClock.advanceTimeBy(300)
            val climbing = capture().red(ys = 0 until 100)
            mainClock.advanceTimeBy(500)
            assertTrue(capture().red(ys = 0 until 150) > climbing, "the burst filled the sky")
        }

    @Test
    fun speedMakesAParticleEffectFaster() =
        runEffectTest {
            val still = ParticlePhysics.of(speed = 0f..0f, gravity = 0f, lifetime = 1f..1f)
            scene(ChangeEffect.spray(squares, 4, still).speed(2f))
            value++
            mainClock.advanceTimeBy(700)
            assertEquals(0, capture().red(), "gone after half the life")
        }

    @Test
    fun confettiOptionsChangeThePieces() =
        runEffectTest {
            val still = ParticlePhysics.of(speed = 0f..0f, gravity = 0f, lifetime = 1f..1f, spin = 0f..0f)
            scene(ChangeEffect.confettiPop(listOf(Color.Red), 6, still, ribbonRatio = 0f, pieceSize = 20.dp))
            value++
            mainClock.advanceTimeBy(150)
            assertTrue(capture().red() > 100, "big pieces")
        }

    @Test
    fun sparkleSpreadPutsStarsOutsideTheElement() =
        runEffectTest {
            scene(ChangeEffect.sparkle(Color.Red, count = 12, spread = 3f, seed = 2))
            value++
            var outside = 0
            repeat(8) {
                mainClock.advanceTimeBy(100)
                val image = capture()
                outside += image.red() - image.red(xs = 150 until 250, ys = 150 until 250)
            }
            assertTrue(outside > 0, "stars land beside the element")
        }

    @Test
    fun speedLineDirectionsKeepToTheirAxis() =
        runEffectTest {
            scene(ChangeEffect.speedLines(Color.Red, 16, directions = SpeedLineDirections.Horizontal))
            value++
            mainClock.advanceTimeBy(200)
            val image = capture()
            assertEquals(0, image.red(xs = 155 until 245, ys = 100 until 145), "nothing above")
            assertEquals(0, image.red(xs = 155 until 245, ys = 255 until 300), "nothing below")
            assertTrue(image.red(xs = 255 until 300) + image.red(xs = 100 until 145) > 0, "lines to the sides")
        }

    @Test
    fun reducedMotionEndStateDrawsNoStandIn() =
        runEffectTest {
            scene(ChangeEffect.spray(squares).reducedMotion(ReducedMotionMode.EndState), KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun parametersAreChecked() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.rise(distance = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.rise(duration = 0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.rise(scaleIn = 2f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.confettiPop(ribbonRatio = 2f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.confettiPop(pieceSize = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.fireworks(rocketHeight = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.fireworks(trailLength = -1f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.sparkle(spread = 0f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.sparkle(duration = 0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.speedLines(taper = 2f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.speedLines(thickness = 0.dp) }
    }

    @Test
    fun equalOptionsMakeEqualEffects() {
        assertEquals(ChangeEffect.spray(seed = 2), ChangeEffect.spray(seed = 2))
        assertNotEquals(ChangeEffect.spray(seed = 2), ChangeEffect.spray(seed = 3))
        assertEquals(ChangeEffect.sparkle(spread = 2f), ChangeEffect.sparkle(spread = 2f))
        assertEquals(ChangeEffect.fireworks(trailLength = 0f), ChangeEffect.fireworks(trailLength = 0f))
    }

    @Test
    fun keyFrameSprayWithPhysics() =
        runEffectTest {
            val fan = ParticlePhysics.of(direction = 0f, spread = 90f, gravity = 300f, colorPick = ColorPick.Random)
            scene(ChangeEffect.spray(ParticleContent.shape(), 16, fan, origin = TransformOrigin(1f, 0.5f), seed = 3))
            value++
            mainClock.advanceTimeBy(300)
            onRoot().captureRoboImage("src/jvmTest/screenshots/spray_physics_300.png")
        }

    @Test
    fun keyFrameSpeedLinesTapered() =
        runEffectTest {
            scene(
                ChangeEffect.speedLines(
                    Color.Red,
                    16,
                    thickness = 6.dp,
                    taper = 1f,
                    directions = SpeedLineDirections.Horizontal,
                ),
            )
            value++
            mainClock.advanceTimeBy(200)
            onRoot().captureRoboImage("src/jvmTest/screenshots/speedlines_tapered_200.png")
        }

    @Test
    fun keyFrameFireworksSmall() =
        runEffectTest {
            scene(ChangeEffect.fireworks(rocketHeight = 70.dp, burstRadius = 50.dp, particleSize = 9.dp, seed = 1))
            value++
            mainClock.advanceTimeBy(800)
            onRoot().captureRoboImage("src/jvmTest/screenshots/fireworks_small_800.png")
        }
}
