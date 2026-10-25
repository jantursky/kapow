package com.jantursky.kapow

import androidx.compose.animation.core.LinearEasing
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
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/** The options of the body and shape effects: spin, burst, stamp and glitch. */
@OptIn(ExperimentalTestApi::class)
class BodyOptionsTest {
    private var value by mutableIntStateOf(0)

    private fun ComposeUiTest.scene(
        effect: ChangeEffect,
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, effect))
            }
        }
    }

    private fun Color.isReddish() = red > 0.6f && green < 0.45f && blue < 0.45f

    private fun Color.isGreenish() = green > 0.5f && red < 0.4f && blue < 0.4f

    private fun Color.isDark() = red < 0.2f && green < 0.2f && blue < 0.2f

    private fun ImageBitmap.count(
        xs: IntRange = 0 until 400,
        ys: IntRange = 0 until 400,
        test: (Color) -> Boolean,
    ): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in ys) for (x in xs) if (test(pixels[x, y])) n++
        return n
    }

    private fun ImageBitmap.pixels(): List<Color> {
        val map = toPixelMap()
        return List(400 * 400) { map[it % 400, it / 400] }
    }

    // ---- spin

    @Test
    fun aSpinWithADurationTurnsAlongItsEasingAndEndsAtRest() =
        runEffectTest {
            scene(ChangeEffect.spin(SpinAxis.Z, turns = 1, duration = 400.milliseconds, easing = LinearEasing))
            value++
            mainClock.advanceTimeBy(60)
            // Part way round, the corners of the square poke out above its top edge.
            val turning = capture()
            assertTrue(turning.count(xs = 150 until 250, ys = 120 until 146) { it != Color.White } > 0)
            mainClock.advanceTimeBy(600)
            val done = capture()
            assertEquals(0, done.count(xs = 150 until 250, ys = 120 until 146) { it != Color.White })
            assertEquals(Cobalt, done.colorAt(160, 160))
        }

    @Test
    fun aSpinCanTurnAroundACorner() {
        fun below(pivot: TransformOrigin): Color {
            var result = Color.Unspecified
            runEffectTest {
                scene(
                    ChangeEffect.spin(
                        SpinAxis.Z,
                        turns = 1,
                        duration = 800.milliseconds,
                        easing = LinearEasing,
                        pivot = pivot,
                    ),
                )
                value++
                mainClock.advanceTimeBy(200)
                result = capture().colorAt(200, 245)
            }
            return result
        }
        assertEquals(Cobalt, below(TransformOrigin.Center), "the middle of the lower half stays covered")
        assertEquals(Color.White, below(TransformOrigin(0f, 0f)), "around the corner it swings away")
    }

    @Test
    fun aReversedSpinSwingsTheOtherWay() {
        fun right(reverse: Boolean): Color {
            var result = Color.Unspecified
            runEffectTest {
                scene(
                    ChangeEffect.spin(
                        SpinAxis.Z,
                        turns = 1,
                        duration = 800.milliseconds,
                        easing = LinearEasing,
                        pivot = TransformOrigin(0f, 0f),
                        reverse = reverse,
                    ),
                )
                value++
                mainClock.advanceTimeBy(100)
                result = capture().colorAt(245, 160)
            }
            return result
        }
        assertEquals(Color.White, right(false), "clockwise swings the top right away")
        assertEquals(Cobalt, right(true), "back the other way it still covers it")
    }

    @Test
    fun noShadingLeavesTheTurningElementItsColor() {
        fun middle(shading: Float): Float {
            var result = 0f
            runEffectTest {
                scene(
                    ChangeEffect.spin(
                        SpinAxis.X,
                        turns = 1,
                        duration = 400.milliseconds,
                        easing = LinearEasing,
                        shading = shading,
                    ),
                )
                value++
                mainClock.advanceTimeBy(150)
                result = capture().colorAt(200, 200).luminance
            }
            return result
        }
        assertTrue(middle(0f) > middle(0.35f) + 0.02f, "the default shades the turning face darker")
    }

    @Test
    fun reducedMotionEndStateDoesNotBrightenASpin() =
        runEffectTest {
            scene(ChangeEffect.spin().reducedMotion(ReducedMotionMode.EndState), KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    // ---- burst

    @Test
    fun aSmallerBurstReachesLessFar() {
        fun far(scale: Float): Int {
            var n = 0
            runEffectTest {
                scene(ChangeEffect.burst(Color.Red, sizeScale = scale))
                value++
                mainClock.advanceTimeBy(180)
                n = capture().count(xs = 0 until 120) { it.isReddish() }
            }
            return n
        }
        assertEquals(0, far(0.5f))
        assertTrue(far(1f) > 0)
    }

    @Test
    fun aBurstInFrontCoversTheElement() {
        fun middle(inFront: Boolean): Color {
            var result = Color.Unspecified
            runEffectTest {
                scene(ChangeEffect.burst(Color.Red, inFront = inFront))
                value++
                mainClock.advanceTimeBy(100)
                result = capture().colorAt(200, 200)
            }
            return result
        }
        assertEquals(Cobalt, middle(false))
        assertTrue(middle(true) != Cobalt, "the burst is drawn over it")
    }

    @Test
    fun theCoreAndOutlineHaveTheirOwnColors() =
        runEffectTest {
            scene(
                ChangeEffect.burst(
                    Color.Red,
                    inFront = true,
                    coreColor = Color.Green,
                    outline = 3.dp,
                    outlineColor = Color.Black,
                ),
            )
            value++
            mainClock.advanceTimeBy(150)
            val image = capture()
            assertTrue(image.count { it.isGreenish() } > 0, "a green core")
            assertTrue(image.count { it.isDark() } > 0, "a dark outline")
        }

    @Test
    fun aBurstCanBeAnchoredElsewhere() {
        fun at(anchor: TransformOrigin): Int {
            var n = 0
            runEffectTest {
                scene(ChangeEffect.burst(Color.Red, anchor = anchor, sizeScale = 0.6f))
                value++
                mainClock.advanceTimeBy(180)
                n = capture().count(xs = 90 until 140, ys = 90 until 140) { it.isReddish() }
            }
            return n
        }
        assertEquals(0, at(TransformOrigin.Center))
        assertTrue(at(TransformOrigin(0f, 0f)) > 0)
    }

    @Test
    fun aShortBurstIsGoneSooner() =
        runEffectTest {
            scene(ChangeEffect.burst(Color.Red, duration = 200.milliseconds))
            value++
            mainClock.advanceTimeBy(350)
            assertEquals(0, capture().count { it.isReddish() })
        }

    // ---- stamp

    @Test
    fun stampDustCanBeLeftOut() {
        fun dust(count: Int): Int {
            var n = 0
            runEffectTest {
                scene(ChangeEffect.stamp(dustColor = Color.Red, dustCount = count))
                value++
                mainClock.advanceTimeBy(260)
                n = capture().count { it.isReddish() }
            }
            return n
        }
        assertEquals(0, dust(0))
        assertTrue(dust(5) > 0)
    }

    @Test
    fun aTiltedStampStartsTurned() {
        fun corner(tilt: Float): Color {
            var result = Color.Unspecified
            runEffectTest {
                scene(ChangeEffect.stamp(scale = 1f, tilt = tilt))
                value++
                mainClock.advanceTimeBy(20)
                result = capture().colorAt(152, 152)
            }
            return result
        }
        assertNotEquals(Color.White, corner(0f))
        assertEquals(Color.White, corner(45f))
    }

    @Test
    fun aStampIsOverWhenItsDurationIs() =
        runEffectTest {
            scene(ChangeEffect.stamp(duration = 280.milliseconds))
            value++
            mainClock.advanceTimeBy(400)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(152, 152))
            assertEquals(Color.White, image.colorAt(148, 200))
        }

    // ---- glitch

    @Test
    fun glitchChannelColorsChangeTheFringes() {
        fun frame(colors: List<Color>): List<Color> {
            var pixels = emptyList<Color>()
            runEffectTest {
                scene(ChangeEffect.glitch(duration = 400.milliseconds, channelColors = colors))
                value++
                mainClock.advanceTimeBy(100)
                pixels = capture().pixels()
            }
            return pixels
        }
        val standard = frame(listOf(Color.Red, Color.Green, Color.Blue))
        assertEquals(standard, frame(listOf(Color.Red, Color.Green, Color.Blue)))
        assertNotEquals(standard, frame(listOf(Color.Green, Color.Green, Color.Green)))
    }

    @Test
    fun theSameGlitchSeedGivesTheSameFrame() {
        fun frame(seed: Int): List<Color> {
            var pixels = emptyList<Color>()
            runEffectTest {
                scene(ChangeEffect.glitch(duration = 400.milliseconds, seed = seed))
                value++
                mainClock.advanceTimeBy(100)
                pixels = capture().pixels()
            }
            return pixels
        }
        assertEquals(frame(2), frame(2))
        assertNotEquals(frame(2), frame(9))
    }

    @Test
    fun noJumpsLeavesTheSlicesInPlace() =
        runEffectTest {
            scene(ChangeEffect.glitch(duration = 400.milliseconds, offset = 0.dp, jumpChance = 0f))
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(200, 200))
            assertEquals(Cobalt, image.colorAt(152, 152))
        }

    // ---- parameters

    @Test
    fun parametersAreChecked() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.spin(turns = 0) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.spin(duration = 0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.spin(shading = 2f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.spin(overshoot = 1f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.spin(cameraDistance = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.burst(sizeScale = 0f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.burst(innerRatio = 2f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.burst(outline = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.burst(duration = 0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.stamp(squash = 2f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.stamp(rebound = 2f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.stamp(dustCount = -1) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.stamp(dustSize = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.stamp(duration = 0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.glitch(sliceHeight = 0f..0.3f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.glitch(channelColors = listOf(Color.Red)) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.glitch(jumpChance = 2f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.glitch(jitterRate = 0f) }
    }

    @Test
    fun equalOptionsMakeEqualEffects() {
        assertEquals(ChangeEffect.spin(turns = 2), ChangeEffect.spin(turns = 2))
        assertNotEquals(ChangeEffect.spin(turns = 2), ChangeEffect.spin(turns = 3))
        assertEquals(ChangeEffect.burst(outline = 2.dp), ChangeEffect.burst(outline = 2.dp))
        assertEquals(ChangeEffect.stamp(tilt = 5f), ChangeEffect.stamp(tilt = 5f))
        assertEquals(ChangeEffect.glitch(seed = 1), ChangeEffect.glitch(seed = 1))
    }

    // ---- frames

    @Test
    fun keyFrameSpinAroundACorner() =
        runEffectTest {
            scene(
                ChangeEffect.spin(
                    SpinAxis.Z,
                    duration = 800.milliseconds,
                    easing = LinearEasing,
                    pivot = TransformOrigin(1f, 1f),
                ),
            )
            value++
            mainClock.advanceTimeBy(150)
            onRoot().captureRoboImage("src/jvmTest/screenshots/spin_pivot_150.png")
        }

    @Test
    fun keyFrameBurstOutlinedInFront() =
        runEffectTest {
            scene(
                ChangeEffect.burst(
                    Color(0xFFFFB547),
                    spikes = 9,
                    coreColor = Color(0xFFFFF3D6),
                    inFront = true,
                    outline = 3.dp,
                    sizeScale = 0.8f,
                    innerRatio = 0.6f,
                ),
            )
            value++
            mainClock.advanceTimeBy(180)
            onRoot().captureRoboImage("src/jvmTest/screenshots/burst_front_180.png")
        }

    @Test
    fun keyFrameStampTilted() =
        runEffectTest {
            scene(ChangeEffect.stamp(tilt = 25f))
            value++
            mainClock.advanceTimeBy(60)
            onRoot().captureRoboImage("src/jvmTest/screenshots/stamp_tilt_60.png")
        }

    @Test
    fun keyFrameGlitchColors() =
        runEffectTest {
            scene(
                ChangeEffect.glitch(
                    duration = 400.milliseconds,
                    offset = 10.dp,
                    channelColors = listOf(Color(0xFFFFB547), Color(0xFF3D7BFF), Color(0xFFFF6B5A)),
                    sliceHeight = 0.05f..0.15f,
                    seed = 3,
                ),
            )
            value++
            mainClock.advanceTimeBy(100)
            onRoot().captureRoboImage("src/jvmTest/screenshots/glitch_colors_100.png")
        }
}
