package com.jantursky.kapow

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The options of the light and outline effects: shine, pulse, glow, ripple, flash, trace and strike. */
@OptIn(ExperimentalTestApi::class)
class LightOptionsTest {
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

    // ---- shine

    @Test
    fun aReversedShineEntersFromTheOtherSide() {
        fun lit(reverse: Boolean): Pair<Float, Float> {
            var result = 0f to 0f
            runEffectTest {
                scene(ChangeEffect.shine(angle = 0f, duration = 1.seconds, easing = LinearEasing, reverse = reverse))
                value++
                mainClock.advanceTimeBy(400)
                val image = capture()
                result = image.colorAt(184, 200).luminance to image.colorAt(235, 200).luminance
            }
            return result
        }
        val (forwardLeft, forwardRight) = lit(false)
        assertTrue(forwardLeft > forwardRight + 0.05f, "forward lights the left first: $forwardLeft $forwardRight")
        val (backLeft, backRight) = lit(true)
        assertTrue(backRight > backLeft + 0.05f, "reverse lights the right first: $backLeft $backRight")
    }

    @Test
    fun aShineWithNoPeakLeavesTheElementAlone() =
        runEffectTest {
            scene(ChangeEffect.shine(angle = 0f, easing = LinearEasing, peakAlpha = 0f))
            value++
            mainClock.advanceTimeBy(500)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun aHardBandIsBrighterAtItsEdge() {
        fun edge(softness: Float): Float {
            var result = 0f
            runEffectTest {
                scene(ChangeEffect.shine(angle = 0f, easing = LinearEasing, softness = softness, bandWidth = 0.6f))
                value++
                mainClock.advanceTimeBy(500)
                // The band is centered on the element's middle, 60 px wide: its quarter point is at x 185.
                result = capture().colorAt(185, 200).luminance
            }
            return result
        }
        assertTrue(edge(0.3f) > edge(1f) + 0.03f)
    }

    // ---- pulse

    @Test
    fun aPulseStopsAtItsReach() {
        fun outside(reach: androidx.compose.ui.unit.Dp): Int {
            var n = 0
            runEffectTest {
                scene(ChangeEffect.pulse(color = Color.Red, style = PulseStyle.Stroke(6.dp), reach = reach))
                value++
                repeat(30) {
                    mainClock.advanceTimeBy(50)
                    n += capture().count(xs = 0 until 126, ys = 190 until 210) { it.isReddish() }
                }
            }
            return n
        }
        assertEquals(0, outside(20.dp), "a 20 dp pulse never reaches 25 dp out")
        assertTrue(outside(60.dp) > 0)
    }

    @Test
    fun aPulseBrushDrawsInsteadOfTheColor() =
        runEffectTest {
            scene(
                ChangeEffect.pulse(color = Color.Red, style = PulseStyle.Stroke(6.dp), brush = SolidColor(Color.Green)),
            )
            value++
            var green = 0
            var red = 0
            repeat(10) {
                mainClock.advanceTimeBy(100)
                val image = capture()
                green += image.count { it.isGreenish() }
                red += image.count { it.isReddish() }
            }
            assertTrue(green > 0, "green pulses")
            assertEquals(0, red)
        }

    @Test
    fun aShortPulseIsOverSooner() =
        runEffectTest {
            scene(ChangeEffect.pulse(color = Color.Red, style = PulseStyle.Stroke(6.dp), duration = 300.milliseconds))
            value++
            mainClock.advanceTimeBy(500)
            assertEquals(0, capture().count { it.isReddish() })
        }

    // ---- glow

    @Test
    fun aGlowWithAFlareAndDecayEndsOnTime() {
        fun halo(effect: ChangeEffect): Float {
            var result = 0f
            runEffectTest {
                scene(effect)
                value++
                mainClock.advanceTimeBy(500)
                result = 1f - capture().colorAt(258, 200).green
            }
            return result
        }
        val spring = halo(ChangeEffect.glow(Color.Red))
        val quick = halo(ChangeEffect.glow(Color.Red, flare = 50.milliseconds, decay = 150.milliseconds))
        assertTrue(spring > 0.05f, "the spring still glows at 500 ms: $spring")
        assertEquals(0f, quick, 0.01f)
    }

    @Test
    fun aGlowWithAFlareReachesItsPeak() =
        runEffectTest {
            scene(ChangeEffect.glow(Color.Red, flare = 100.milliseconds, decay = 600.milliseconds))
            value++
            mainClock.advanceTimeBy(120)
            val halo = capture().colorAt(258, 200)
            assertTrue(halo.red > halo.blue + 0.05f, "reddish halo: $halo")
        }

    @Test
    fun noIntensityMeansNoHalo() =
        runEffectTest {
            scene(ChangeEffect.glow(Color.Red, intensity = 0f))
            value++
            mainClock.advanceTimeBy(250)
            assertEquals(Color.White, capture().colorAt(258, 200))
        }

    @Test
    fun aGlowOffsetMovesTheHalo() =
        runEffectTest {
            scene(ChangeEffect.glow(Color.Red, offset = DpOffset(14.dp, 0.dp)))
            value++
            mainClock.advanceTimeBy(250)
            val image = capture()
            val right = 1f - image.colorAt(262, 200).green
            val left = 1f - image.colorAt(138, 200).green
            assertTrue(right > left + 0.05f, "the halo leans right: $left $right")
        }

    @Test
    fun anInnerGlowTintsTheEdgeInside() {
        fun edge(inner: Float): Color {
            var result = Color.Unspecified
            runEffectTest {
                scene(ChangeEffect.glow(Color.Red, innerGlow = inner))
                value++
                mainClock.advanceTimeBy(250)
                result = capture().colorAt(153, 200)
            }
            return result
        }
        val plain = edge(0f)
        val inner = edge(1f)
        assertTrue(inner.red > plain.red + 0.1f, "redder inside the edge: $plain $inner")
    }

    @Test
    fun reducedMotionEndStateShowsNoGlow() =
        runEffectTest {
            scene(ChangeEffect.glow(Color.Red).reducedMotion(ReducedMotionMode.EndState), KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    // ---- ripple

    @Test
    fun aRingRippleLeavesItsMiddleClear() =
        runEffectTest {
            scene(ChangeEffect.ripple(Color.Red, ringWidth = 6.dp, duration = 600.milliseconds))
            value++
            mainClock.advanceTimeBy(300)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(200, 200), "no fill in the middle")
            assertTrue(image.count(xs = 150 until 250, ys = 150 until 250) { it.isReddish() } > 20, "a ring somewhere")
        }

    @Test
    fun aRippleStopsAtItsMaxRadius() {
        fun corner(maxRadius: androidx.compose.ui.unit.Dp): Color {
            var result = Color.Unspecified
            runEffectTest {
                scene(ChangeEffect.ripple(Color.Red, maxRadius = maxRadius, duration = 400.milliseconds))
                value++
                mainClock.advanceTimeBy(350)
                result = capture().colorAt(153, 153)
            }
            return result
        }
        assertEquals(Cobalt, corner(20.dp))
        assertNotEquals(Cobalt, corner(androidx.compose.ui.unit.Dp.Unspecified))
    }

    @Test
    fun severalRingsMakeMoreRingPixels() {
        fun rings(count: Int): Int {
            var n = 0
            runEffectTest {
                scene(ChangeEffect.ripple(Color.Red, ringWidth = 3.dp, rings = count, duration = 800.milliseconds))
                value++
                mainClock.advanceTimeBy(400)
                n = capture().count(xs = 150 until 250, ys = 150 until 250) { it.isReddish() }
            }
            return n
        }
        assertTrue(rings(3) > rings(1))
    }

    // ---- flash

    @Test
    fun strobesFlashTwiceInTheSameTime() {
        fun middle(strobes: Int): Float {
            var result = 0f
            runEffectTest {
                scene(ChangeEffect.flash(Color.White, 400.milliseconds, strobes = strobes))
                value++
                mainClock.advanceTimeBy(190)
                result = capture().colorAt(200, 200).luminance
            }
            return result
        }
        val single = middle(1)
        val double = middle(2)
        assertTrue(single > Cobalt.luminance + 0.05f, "one flash is still going at half time")
        assertTrue(double < single - 0.05f, "two flashes are between strobes: $double against $single")
    }

    @Test
    fun aFlashCanBeLimitedToAShape() =
        runEffectTest {
            scene(ChangeEffect.flash(Color.White, 400.milliseconds, shape = CircleShape))
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertTrue(image.colorAt(200, 200).luminance > Cobalt.luminance + 0.05f, "lit in the circle")
            assertEquals(Cobalt, image.colorAt(152, 152))
        }

    @Test
    fun reducedMotionEndStateSkipsTheFlash() =
        runEffectTest {
            scene(ChangeEffect.flash().reducedMotion(ReducedMotionMode.EndState), KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(40)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    // ---- trace

    @Test
    fun aTraceCanRunTheOtherWay() {
        fun lit(clockwise: Boolean): Pair<Boolean, Boolean> {
            var result = false to false
            runEffectTest {
                scene(
                    ChangeEffect.trace(
                        color = Color.Red,
                        duration = 1.seconds,
                        easing = LinearEasing,
                        clockwise = clockwise,
                    ),
                )
                value++
                mainClock.advanceTimeBy(100)
                val image = capture()
                val right = image.count(xs = 205 until 250, ys = 150 until 158) { it.isReddish() } > 0
                val left = image.count(xs = 150 until 195, ys = 150 until 158) { it.isReddish() } > 0
                result = right to left
            }
            return result
        }
        assertEquals(true to false, lit(true))
        assertEquals(false to true, lit(false))
    }

    @Test
    fun aTraceCanStartHalfwayRound() =
        runEffectTest {
            scene(ChangeEffect.trace(color = Color.Red, duration = 1.seconds, easing = LinearEasing, startAt = 0.5f))
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(0, image.count(xs = 150 until 250, ys = 150 until 158) { it.isReddish() }, "not at the top")
            assertTrue(image.count(xs = 150 until 250, ys = 242 until 250) { it.isReddish() } > 0, "at the bottom")
        }

    @Test
    fun aTraceBrushDrawsInsteadOfTheColor() =
        runEffectTest {
            scene(
                ChangeEffect.trace(
                    color = Color.Red,
                    brush = SolidColor(Color.Green),
                    duration = 1.seconds,
                    easing = LinearEasing,
                ),
            )
            value++
            mainClock.advanceTimeBy(150)
            val image = capture()
            assertTrue(image.count { it.isGreenish() } > 0)
            assertEquals(0, image.count { it.isReddish() })
        }

    @Test
    fun moreLapsKeepTheCometGoingAfterOneRound() {
        fun after(laps: Int): Int {
            var n = 0
            runEffectTest {
                scene(ChangeEffect.trace(color = Color.Red, duration = 2.seconds, easing = LinearEasing, laps = laps))
                value++
                mainClock.advanceTimeBy(1000)
                n = capture().count { it.isReddish() }
            }
            return n
        }
        // Halfway through a single lap the comet is at the bottom; halfway through two it is back at the top.
        assertTrue(after(1) > 0)
        assertTrue(after(2) > 0)
    }

    // ---- strike

    @Test
    fun aVerticalStrikeDrawsDownTheMiddle() =
        runEffectTest {
            scene(ChangeEffect.strike(Color.Red, 4.dp, direction = StrikeDirection.Vertical, easing = LinearEasing))
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertTrue(image.colorAt(200, 156).isReddish(), "from the top")
            assertEquals(Cobalt, image.colorAt(155, 200))
            assertEquals(Cobalt, image.colorAt(200, 245), "not yet at the bottom")
        }

    @Test
    fun aReversedStrikeStartsAtTheEndEdge() =
        runEffectTest {
            scene(ChangeEffect.strike(Color.Red, 4.dp, direction = StrikeDirection.Reverse, easing = LinearEasing))
            value++
            mainClock.advanceTimeBy(80)
            val image = capture()
            assertTrue(image.colorAt(247, 200).isReddish())
            assertEquals(Cobalt, image.colorAt(152, 200))
        }

    @Test
    fun aDiagonalStrikeCrossesTheElementCornerToCorner() =
        runEffectTest {
            scene(
                ChangeEffect.strike(
                    Color.Red,
                    4.dp,
                    direction = StrikeDirection.Diagonal,
                    easing = LinearEasing,
                    hold = 2.seconds,
                ),
            )
            value++
            mainClock.advanceTimeBy(600)
            val image = capture()
            assertTrue(image.colorAt(200, 200).isReddish(), "through the middle")
            assertTrue(image.colorAt(170, 170).isReddish())
            assertEquals(Cobalt, image.colorAt(170, 230))
        }

    @Test
    fun anOffsetStrikeMovesDown() =
        runEffectTest {
            scene(ChangeEffect.strike(Color.Red, 4.dp, offset = 0.25f, hold = 2.seconds))
            value++
            mainClock.advanceTimeBy(500)
            val image = capture()
            assertTrue(image.colorAt(200, 225).isReddish())
            assertEquals(Cobalt, image.colorAt(200, 200))
        }

    @Test
    fun severalStrikeLinesRunSideBySide() =
        runEffectTest {
            scene(ChangeEffect.strike(Color.Red, 4.dp, hold = 2.seconds, lines = 2, lineSpacing = 12.dp))
            value++
            mainClock.advanceTimeBy(500)
            val image = capture()
            assertTrue(image.colorAt(200, 194).isReddish())
            assertTrue(image.colorAt(200, 206).isReddish())
            assertEquals(Cobalt, image.colorAt(200, 200))
        }

    @Test
    fun aWavyStrikeLeavesTheStraightLine() =
        runEffectTest {
            scene(ChangeEffect.strike(Color.Red, 3.dp, hold = 2.seconds, waveAmplitude = 8.dp, waveLength = 24.dp))
            value++
            mainClock.advanceTimeBy(500)
            val image = capture()
            assertTrue(
                image.count(xs = 150 until 250, ys = 205 until 212) { it.isReddish() } > 0,
                "swings below the line",
            )
            assertTrue(image.count(xs = 150 until 250, ys = 188 until 195) { it.isReddish() } > 0, "and above it")
        }

    // ---- parameters

    @Test
    fun parametersAreChecked() {
        assertFailsWith<IllegalArgumentException> { ChangeEffect.shine(bandWidth = 0f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.shine(softness = 0f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.shine(peakAlpha = 2f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.shine(bands = 0) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.pulse(duration = 0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.pulse(startInset = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.pulse(interval = (-1).milliseconds) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.glow(intensity = 2f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.glow(innerGlow = -1f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.glow(flare = 0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.glow(stackLimit = 0f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.ripple(rings = 0) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.ripple(rings = 6, ringSpacing = 0.3f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.ripple(ringWidth = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.flash(strobes = 0) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.trace(tail = 2f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.trace(laps = 0) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.trace(startAt = 1.5f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.strike(offset = 1f) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.strike(lines = 0) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.strike(drawDuration = 0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { ChangeEffect.strike(waveLength = 0.dp) }
    }

    @Test
    fun equalOptionsMakeEqualEffects() {
        assertEquals(ChangeEffect.shine(bands = 2), ChangeEffect.shine(bands = 2))
        assertNotEquals(ChangeEffect.shine(bands = 2), ChangeEffect.shine(bands = 3))
        assertEquals(ChangeEffect.glow(innerGlow = 0.5f), ChangeEffect.glow(innerGlow = 0.5f))
        assertEquals(ChangeEffect.trace(laps = 2), ChangeEffect.trace(laps = 2))
        assertEquals(ChangeEffect.strike(lines = 2), ChangeEffect.strike(lines = 2))
    }

    // ---- frames

    @Test
    fun keyFrameShineBands() =
        runEffectTest {
            scene(ChangeEffect.shine(angle = 20f, easing = LinearEasing, bands = 3, bandWidth = 0.25f, softness = 0.6f))
            value++
            mainClock.advanceTimeBy(500)
            onRoot().captureRoboImage("src/jvmTest/screenshots/shine_bands_500.png")
        }

    @Test
    fun keyFramePulseRingsWithBrush() =
        runEffectTest {
            val brush =
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(Color(0xFFFF6B5A), Color(0xFF6B62F2)),
                )
            scene(
                ChangeEffect.pulse(
                    shape = CircleShape,
                    style = PulseStyle.Stroke(4.dp),
                    count = 3,
                    brush = brush,
                    reach = 40.dp,
                    startInset = 10.dp,
                ),
            )
            value++
            mainClock.advanceTimeBy(500)
            onRoot().captureRoboImage("src/jvmTest/screenshots/pulse_rings_500.png")
        }

    @Test
    fun keyFrameGlowInnerOffset() =
        runEffectTest {
            scene(ChangeEffect.glow(Color(0xFFFFB547), innerGlow = 0.8f, offset = DpOffset(0.dp, 8.dp), spread = 6.dp))
            value++
            mainClock.advanceTimeBy(250)
            onRoot().captureRoboImage("src/jvmTest/screenshots/glow_inner_250.png")
        }

    @Test
    fun keyFrameRippleRings() =
        runEffectTest {
            scene(ChangeEffect.ripple(Color(0xFFFFB547), ringWidth = 4.dp, rings = 3, maxRadius = 90.dp))
            value++
            mainClock.advanceTimeBy(350)
            onRoot().captureRoboImage("src/jvmTest/screenshots/ripple_rings_350.png")
        }

    @Test
    fun keyFrameFlashStrobeCircle() =
        runEffectTest {
            scene(ChangeEffect.flash(Color(0xFFFFB547), 600.milliseconds, strobes = 3, shape = CircleShape))
            value++
            mainClock.advanceTimeBy(40)
            onRoot().captureRoboImage("src/jvmTest/screenshots/flash_circle_40.png")
        }

    @Test
    fun keyFrameTraceBackwardsWithBrush() =
        runEffectTest {
            val brush =
                androidx.compose.ui.graphics.Brush.sweepGradient(
                    listOf(Color(0xFFFFB547), Color(0xFFFF6B5A), Color(0xFF6B62F2)),
                )
            scene(
                ChangeEffect.trace(
                    width = 5.dp,
                    duration = 1.seconds,
                    easing = LinearEasing,
                    clockwise = false,
                    startAt = 0.25f,
                    tail = 0.6f,
                    brush = brush,
                ),
            )
            value++
            mainClock.advanceTimeBy(350)
            onRoot().captureRoboImage("src/jvmTest/screenshots/trace_route_350.png")
        }

    @Test
    fun keyFrameStrikeWavyLines() =
        runEffectTest {
            scene(
                ChangeEffect.strike(
                    Color(0xFFFFB547),
                    3.dp,
                    hold = 2.seconds,
                    direction = StrikeDirection.Diagonal,
                    waveAmplitude = 4.dp,
                    waveLength = 20.dp,
                    lines = 2,
                    lineSpacing = 14.dp,
                ),
            )
            value++
            mainClock.advanceTimeBy(500)
            onRoot().captureRoboImage("src/jvmTest/screenshots/strike_wavy_500.png")
        }
}
