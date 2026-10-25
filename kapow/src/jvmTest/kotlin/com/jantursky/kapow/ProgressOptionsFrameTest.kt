package com.jantursky.kapow

import androidx.compose.animation.core.Easing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProgressOptionsFrameTest {
    /** The frame of [effect] on the element at (150, 150) when the progress is [at]. */
    private fun frameOf(
        effect: ProgressEffect,
        at: Float,
    ): ImageBitmap {
        var image: ImageBitmap? = null
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    SolidElement(Modifier.offset(150.dp, 150.dp).progressEffect(effect) { at })
                }
            }
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            image = capture()
        }
        return image!!
    }

    private fun Color.isReddish() = red > 0.6f && blue < 0.6f

    @Test
    fun moreBandsAndAWiderBandChangeTheShine() {
        val one = frameOf(ProgressEffect.shine(angle = 0f), 0.5f)
        val two = frameOf(ProgressEffect.shine(angle = 0f, bands = 2), 0.5f)
        assertTrue(one.pixelsDifferentFrom(two) > 0)

        fun lit(image: ImageBitmap) =
            (150 until 250).count {
                image.colorAt(it, 200).luminance >
                    Cobalt.luminance + 0.05f
            }
        val narrow = lit(frameOf(ProgressEffect.shine(angle = 0f, bandWidth = 0.2f), 0.5f))
        val wide = lit(frameOf(ProgressEffect.shine(angle = 0f, bandWidth = 1f), 0.5f))
        assertTrue(wide > narrow, "$wide vs $narrow")
    }

    @Test
    fun theShineKeepsToTheElementUnlessTheBlendModeSaysOtherwise() {
        val onElement = frameOf(ProgressEffect.shine(angle = 0f, bandWidth = 1.2f), 0.5f)
        assertEquals(Color.White, onElement.colorAt(100, 200), "nothing outside the element")
        val screened =
            frameOf(
                ProgressEffect.shine(angle = 0f, color = Color.Black, bandWidth = 1.2f, blendMode = BlendMode.Src),
                0.5f,
            )
        assertTrue(screened.pixelsDifferentFrom(onElement) > 0)
    }

    @Test
    fun theGlowFollowsAColorRampAndAnAlphaCurve() {
        val ramp = listOf(Color.Red, Color.Blue)
        val halo = frameOf(ProgressEffect.glow(radius = 24.dp, colorRamp = ramp), 1f).colorAt(146, 200)
        assertTrue(halo.blue > halo.red, "blue at the end of the ramp: $halo")
        val early = frameOf(ProgressEffect.glow(radius = 24.dp, colorRamp = ramp), 0.3f).colorAt(146, 200)
        assertTrue(early.red > early.blue, "redder early: $early")
        val none = Easing { 0f }
        assertEquals(Color.White, frameOf(ProgressEffect.glow(radius = 24.dp, alphaCurve = none), 1f).colorAt(146, 200))
    }

    @Test
    fun aSoftIrisBlendsItsEdgeAndStillEndsFull() {
        val hard = frameOf(ProgressEffect.iris(), 0.5f)
        val soft = frameOf(ProgressEffect.iris(softEdge = 40.dp), 0.5f)
        // Somewhere along the diagonal the soft edge is neither element nor white.
        val blended =
            (0 until 70).count { i ->
                val c = soft.colorAt(200 + i, 200 + i)
                c != Cobalt && c != Color.White
            }
        val hardBlended =
            (0 until 70).count { i ->
                val c = hard.colorAt(200 + i, 200 + i)
                c != Cobalt && c != Color.White
            }
        assertTrue(blended > hardBlended + 5, "soft: $blended, hard: $hardBlended")
        val full = frameOf(ProgressEffect.iris(softEdge = 40.dp), 1f)
        assertEquals(Cobalt, full.colorAt(151, 151))
        assertEquals(Cobalt, full.colorAt(248, 248))
        assertEquals(Color.White, frameOf(ProgressEffect.iris(softEdge = 40.dp), 0f).colorAt(200, 200))
    }

    @Test
    fun anInvertedIrisHidesTheElementFromTheMiddle() {
        val effect = ProgressEffect.iris(invert = true)
        assertEquals(Cobalt, frameOf(effect, 0f).colorAt(200, 200), "shown at 0")
        val half = frameOf(effect, 0.5f)
        assertEquals(Color.White, half.colorAt(200, 200), "hole in the middle")
        assertEquals(Cobalt, half.colorAt(152, 152), "corners still shown")
        assertEquals(Color.White, frameOf(effect, 1f).colorAt(152, 152), "gone at 1")
    }

    @Test
    fun anIrisCanRevealThroughAShape() {
        val round = frameOf(ProgressEffect.iris(), 0.5f)
        val square = frameOf(ProgressEffect.iris(shape = RectangleShape), 0.5f)
        assertEquals(Color.White, round.colorAt(230, 230), "outside the circle")
        assertEquals(Cobalt, square.colorAt(230, 230), "inside the square")
        assertEquals(Cobalt, frameOf(ProgressEffect.iris(shape = CircleShape), 1f).colorAt(152, 152))
    }

    @Test
    fun theLiquidCanRiseInAnyDirection() {
        val tint = Color(0xFFFF0000)

        fun tinted(
            image: ImageBitmap,
            x: Int,
            y: Int,
        ) = image.colorAt(x, y).red > image.colorAt(x, y).blue
        val right = frameOf(ProgressEffect.fill(tint, wave = 0.dp, direction = FillDirection.Right), 0.5f)
        assertTrue(tinted(right, 170, 200) && !tinted(right, 230, 200), "from the left")
        val left = frameOf(ProgressEffect.fill(tint, wave = 0.dp, direction = FillDirection.Left), 0.5f)
        assertTrue(tinted(left, 230, 200) && !tinted(left, 170, 200), "from the right")
        val down = frameOf(ProgressEffect.fill(tint, wave = 0.dp, direction = FillDirection.Down), 0.5f)
        assertTrue(tinted(down, 200, 170) && !tinted(down, 200, 230), "from the top")
        val up = frameOf(ProgressEffect.fill(tint, wave = 0.dp), 0.5f)
        assertTrue(tinted(up, 200, 230) && !tinted(up, 200, 170), "from the bottom")
    }

    @Test
    fun theLiquidFollowsAColorRampAndRidesFoam() {
        val ramp = listOf(Color(0xFFFF0000), Color(0xFF00FF00))
        val low = frameOf(ProgressEffect.fill(Color.Transparent, wave = 0.dp, colorRamp = ramp), 0.3f).colorAt(200, 240)
        val high =
            frameOf(
                ProgressEffect.fill(Color.Transparent, wave = 0.dp, colorRamp = ramp),
                0.9f,
            ).colorAt(200, 240)
        assertTrue(low.red > low.green, "red early: $low")
        assertTrue(high.green > high.red, "green late: $high")
        val plain = frameOf(ProgressEffect.fill(Color(0x99FF0000), wave = 0.dp), 0.5f)
        val foamy = frameOf(ProgressEffect.fill(Color(0x99FF0000), wave = 0.dp, foam = 6.dp), 0.5f)
        assertTrue(foamy.colorAt(200, 200).green > plain.colorAt(200, 200).green + 0.1f, "a pale line at the surface")
        assertEquals(plain.colorAt(200, 240), foamy.colorAt(200, 240), "the rest is the same")
    }

    @Test
    fun aTraceCanGoTheOtherWayAndStartElsewhere() {
        val red = Color.Red
        val cw = frameOf(ProgressEffect.trace(color = red, width = 4.dp), 0.25f)
        assertTrue(cw.colorAt(230, 152).isReddish() && !cw.colorAt(170, 152).isReddish(), "clockwise from the top")
        val ccw = frameOf(ProgressEffect.trace(color = red, width = 4.dp, clockwise = false), 0.25f)
        assertTrue(ccw.colorAt(170, 152).isReddish() && !ccw.colorAt(230, 152).isReddish(), "counterclockwise")
        val bottom = frameOf(ProgressEffect.trace(color = red, width = 4.dp, start = 0.5f), 0.25f)
        assertTrue(bottom.colorAt(170, 248).isReddish(), "from the bottom going left")
        assertTrue(!bottom.colorAt(230, 152).isReddish())
    }

    @Test
    fun aShortTailTravelsWithItsHead() {
        val head = frameOf(ProgressEffect.trace(color = Color.Red, width = 4.dp, tail = 0.1f), 0.5f)
        assertTrue(
            head.colorAt(248, 200).isReddish() || head.colorAt(200, 248).isReddish(),
            "the head is at the bottom right",
        )
        assertTrue(!head.colorAt(220, 152).isReddish(), "the start is clear")
        val whole = frameOf(ProgressEffect.trace(color = Color.Red, width = 4.dp), 0.5f)
        assertTrue(whole.colorAt(220, 152).isReddish())
    }

    @Test
    fun aTraceTakesABrushAndACap() {
        val brush = Brush.linearGradient(listOf(Color.Green, Color.Green))
        val green = frameOf(ProgressEffect.trace(brush = brush, width = 4.dp), 1f).colorAt(220, 152)
        assertTrue(green.green > green.red + 0.3f, "green stroke: $green")
        val round = frameOf(ProgressEffect.trace(color = Color.Red, width = 12.dp, cap = StrokeCap.Round), 0.25f)
        val butt = frameOf(ProgressEffect.trace(color = Color.Red, width = 12.dp, cap = StrokeCap.Butt), 0.25f)
        assertTrue(round.pixelsDifferentFrom(butt) > 0)
    }
}
