package com.jantursky.kapow

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class ConditionalLookOptionsFrameTest {
    /** The frames of [effect] at each of [times] milliseconds after it turns on. */
    private fun framesOf(
        effect: ConditionalEffect,
        vararg times: Long,
        motion: KapowMotion = KapowMotion.Full,
    ): List<ImageBitmap> {
        val frames = mutableListOf<ImageBitmap>()
        val scene = LoopScene()
        runEffectTest {
            scene.show(this, effect, motion)
            scene.on = true
            var now = 0L
            for (t in times) {
                mainClock.advanceTimeBy(t - now)
                now = t
                frames += capture()
            }
        }
        return frames
    }

    private fun frameOf(
        effect: ConditionalEffect,
        millis: Long = 800,
        motion: KapowMotion = KapowMotion.Full,
    ): ImageBitmap = framesOf(effect, millis, motion = motion).single()

    @Test
    fun aBreathingGlowChangesOverTimeAndASteadyOneDoesNot() {
        val steady = framesOf(ConditionalEffect.glow(radius = 24.dp), 800, 1300)
        assertEquals(0, steady[0].pixelsDifferentFrom(steady[1]))
        val breathing = framesOf(ConditionalEffect.glow(radius = 24.dp, breathe = 0.8f, period = 2.seconds), 800, 1300)
        assertTrue(breathing[0].pixelsDifferentFrom(breathing[1]) > 0)
    }

    @Test
    fun aStrongerGlowReachesFurther() {
        fun lit(image: ImageBitmap) = (130 until 150).count { image.colorAt(it, 200) != Color.White }
        val weak = lit(frameOf(ConditionalEffect.glow(radius = 24.dp, strength = 0.3f)))
        val strong = lit(frameOf(ConditionalEffect.glow(radius = 24.dp, strength = 1.2f)))
        assertTrue(strong > weak, "$strong vs $weak")
    }

    @Test
    fun pushDownCanMoveAndDimTheElement() {
        val plain = frameOf(ConditionalEffect.pushDown(), 600)
        val pressed = frameOf(ConditionalEffect.pushDown(translation = 12.dp, dim = 0.5f), 600)
        assertEquals(Color.White, plain.colorAt(200, 255), "nothing below the plain element")
        assertTrue(pressed.colorAt(200, 255) != Color.White, "moved down past the old bottom edge")
        assertTrue(pressed.colorAt(200, 200).luminance > plain.colorAt(200, 200).luminance, "dimmed toward white")
    }

    @Test
    fun pushDownShrinksTowardItsOrigin() {
        val image = frameOf(ConditionalEffect.pushDown(scale = 0.8f, origin = TransformOrigin(0f, 0f)), 600)
        assertEquals(Cobalt, image.colorAt(152, 152), "the top left corner stays")
        assertEquals(Color.White, image.colorAt(248, 248), "the bottom right corner pulled in")
    }

    @Test
    fun aBeamTakesABrushForItsTrack() {
        val green =
            frameOf(
                ConditionalEffect.borderBeam(
                    brush = Brush.linearGradient(listOf(Color.Green, Color.Green)),
                    trackAlpha = 1f,
                    width = 4.dp,
                ),
                500,
            )
        val track = green.colorAt(151, 200)
        assertTrue(track.green > track.red + 0.3f, "a green track: $track")
    }

    @Test
    fun moreBeamsAndAnotherDirectionChangeTheFrame() {
        val one = frameOf(ConditionalEffect.borderBeam(Color.Red, width = 4.dp), 700)
        val two = frameOf(ConditionalEffect.borderBeam(Color.Red, width = 4.dp, beams = 2), 700)
        val back = frameOf(ConditionalEffect.borderBeam(Color.Red, width = 4.dp, clockwise = false), 700)
        assertTrue(one.pixelsDifferentFrom(two) > 0)
        assertTrue(one.pixelsDifferentFrom(back) > 0)
    }

    @Test
    fun theTrackAndTheHaloFollowTheirOptions() {
        val bright = frameOf(ConditionalEffect.borderBeam(Color.Red, width = 4.dp, trackAlpha = 0.6f), 700)
        val faint = frameOf(ConditionalEffect.borderBeam(Color.Red, width = 4.dp, trackAlpha = 0f, glow = 0f), 700)
        assertTrue(bright.colorAt(151, 200).red > faint.colorAt(151, 200).red + 0.1f, "a brighter track")
    }

    @Test
    fun antsCanRunTheOtherWayAndWearRoundCaps() {
        val forward = frameOf(ConditionalEffect.marchingAnts(), 500)
        val backward = frameOf(ConditionalEffect.marchingAnts(clockwise = false), 500)
        val round = frameOf(ConditionalEffect.marchingAnts(roundCaps = true, width = 4.dp), 500)
        val square = frameOf(ConditionalEffect.marchingAnts(width = 4.dp), 500)
        assertTrue(forward.pixelsDifferentFrom(backward) > 0)
        assertTrue(round.pixelsDifferentFrom(square) > 0)
    }

    @Test
    fun theStartOffsetShiftsTheDashes() {
        val plain = frameOf(ConditionalEffect.marchingAnts(), 500)
        val shifted = frameOf(ConditionalEffect.marchingAnts(dashOffset = 3.dp), 500)
        assertTrue(plain.pixelsDifferentFrom(shifted) > 0)
    }

    @Test
    fun antsTakeABrush() {
        val image =
            frameOf(
                ConditionalEffect.marchingAnts(
                    gapColor = Color.Transparent,
                    brush = Brush.linearGradient(listOf(Color.Red, Color.Red)),
                    width = 6.dp,
                ),
                500,
                KapowMotion.Reduced,
            )
        val dashed = (150 until 250).map { image.colorAt(it, 152) }
        assertTrue(dashed.any { it.red > 0.9f && it.green < 0.2f }, "red dashes along the top edge")
    }

    @Test
    fun theRainbowCanBeFaintGlowingAndTurnTheOtherWay() {
        val solid = frameOf(ConditionalEffect.rainbowBorder(), 500)
        val faint = frameOf(ConditionalEffect.rainbowBorder(alpha = 0.3f), 500)

        fun fromElement(c: Color) = kotlin.math.abs(c.red - Cobalt.red) + kotlin.math.abs(c.green - Cobalt.green)
        assertTrue(fromElement(faint.colorAt(151, 200)) < fromElement(solid.colorAt(151, 200)), "closer to the element")
        val glowing = frameOf(ConditionalEffect.rainbowBorder(glow = 8.dp), 500)
        assertTrue(glowing.colorAt(146, 200) != Color.White, "a halo outside the element")
        assertEquals(Color.White, solid.colorAt(146, 200))
        val back = frameOf(ConditionalEffect.rainbowBorder(clockwise = false), 900)
        val forward = frameOf(ConditionalEffect.rainbowBorder(), 900)
        assertTrue(back.pixelsDifferentFrom(forward) > 0)
    }

    @Test
    fun grayscaleCanTintAndDim() {
        val gray = frameOf(ConditionalEffect.grayscale(), 600).colorAt(200, 200)
        val tinted = frameOf(ConditionalEffect.grayscale(tint = Color(0xFFB08850)), 600).colorAt(200, 200)
        assertTrue(tinted.red > tinted.blue + 0.05f, "warm: $tinted")
        assertTrue(kotlin.math.abs(gray.red - gray.blue) < 0.02f, "neutral gray: $gray")
        val dimmed = frameOf(ConditionalEffect.grayscale(dim = 0.5f), 600).colorAt(200, 200)
        assertTrue(dimmed.luminance < gray.luminance - 0.05f, "darker: $dimmed vs $gray")
    }

    @Test
    fun theSpotlightCanDrawARing() {
        val plain = frameOf(ConditionalEffect.spotlight(padding = 8.dp, softness = 0.dp), 500)
        val ringed =
            frameOf(
                ConditionalEffect.spotlight(padding = 8.dp, softness = 0.dp, ring = 4.dp, ringColor = Color.Green),
                500,
            )
        val ring = ringed.colorAt(142, 200)
        assertTrue(ring.green > ring.red + 0.2f, "green ring at the edge of the clear area: $ring")
        assertTrue(plain.colorAt(142, 200).green <= plain.colorAt(142, 200).red + 0.05f)
    }

    @Test
    fun theSpotlightBreathes() {
        val steady = framesOf(ConditionalEffect.spotlight(padding = 12.dp, softness = 0.dp), 600, 900)
        assertEquals(0, steady[0].pixelsDifferentFrom(steady[1]))
        val breathing =
            framesOf(
                ConditionalEffect.spotlight(padding = 12.dp, softness = 0.dp, breathe = 8.dp, period = 1.seconds),
                600,
                850,
            )
        assertTrue(breathing[0].pixelsDifferentFrom(breathing[1]) > 0)
    }

    @Test
    fun repeatWaitsForTheInitialDelay() {
        val effect = RecordingEffect()
        val scene = LoopScene()
        runEffectTest {
            scene.show(this, ConditionalEffect.repeat(effect, every = 2.seconds, initialDelay = 500.milliseconds))
            scene.on = true
            mainClock.advanceTimeBy(300)
            assertEquals(0, effect.log.size)
            mainClock.advanceTimeBy(400)
            assertEquals(1, effect.log.size)
        }
    }

    @Test
    fun repeatStopsAtTheMaxCountAndStartsOverWhenTurnedOnAgain() {
        val effect = RecordingEffect()
        val scene = LoopScene()
        runEffectTest {
            scene.show(this, ConditionalEffect.repeat(effect, every = 500.milliseconds, maxCount = 2))
            scene.on = true
            mainClock.advanceTimeBy(3000)
            assertEquals(2, effect.log.size)
            scene.on = false
            mainClock.advanceTimeBy(100)
            scene.on = true
            mainClock.advanceTimeBy(3000)
            assertEquals(4, effect.log.size)
        }
    }

    @Test
    fun repeatJitterKeepsEveryWaitWithinItsBound() {
        val effect = RecordingEffect()
        val scene = LoopScene()
        runEffectTest {
            scene.show(this, ConditionalEffect.repeat(effect, every = 1.seconds, jitter = 0.5f, seed = 7))
            scene.on = true
            mainClock.advanceTimeBy(10_000)
            // Waits of 0.5 to 1.5 s: between 7 and 20 plays in 10 s, and not exactly the unjittered 10.
            assertTrue(effect.log.size in 7..21, "plays: ${effect.log.size}")
        }
    }

    @Test
    fun aSlowerFadeInKeepsTheGrayscaleColoredLonger() {
        val quick = frameOf(ConditionalEffect.grayscale(), 150).colorAt(200, 200)
        val slow = frameOf(ConditionalEffect.grayscale().fadeIn(2.seconds), 150).colorAt(200, 200)
        assertTrue(slow.blue - slow.red > quick.blue - quick.red, "$slow vs $quick")
    }
}
