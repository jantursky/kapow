package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MaskOptionsTest {
    /** The share of pixels of the element at [millis] into [enter]. */
    private fun TransitionScene.blendedAt(
        enter: KapowEnter,
        millis: Long,
    ): Int {
        set(enter = enter)
        show()
        advance(millis)
        return others()
    }

    private fun ImageBitmap.differsFrom(other: ImageBitmap): Int {
        val a = toPixelMap()
        val b = other.toPixelMap()
        var n = 0
        for (y in 0 until 400) for (x in 0 until 400) if (a[x, y] != b[x, y]) n++
        return n
    }

    private fun ImageBitmap.lighter(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 150 until 250) {
            for (x in 150 until 250) {
                if (pixels[x, y] != Color.White &&
                    pixels[x, y].red > Cobalt.red + 0.05f
                ) {
                    n++
                }
            }
        }
        return n
    }

    @Test
    fun aSoftIrisBlendsItsEdgeAndEndsWhole() {
        var sharp = 0
        runTransitionTest { sharp = blendedAt(KapowEnter.iris(), 200) }
        runTransitionTest {
            val soft = blendedAt(KapowEnter.iris(softEdge = 40.dp), 200)
            assertTrue(soft > sharp + 300, "a soft edge has many blended pixels: $soft against $sharp")
            advance(3000)
            assertEquals(1f, shown(), "fully shown at the end")
            assertEquals(0, others())
        }
    }

    @Test
    fun aSoftIrisExitLeavesNothing() =
        runTransitionTest {
            hideFromShown(KapowExit.iris(softEdge = 40.dp))
            advance(200)
            assertTrue(others() > 300)
            advance(3000)
            assertEquals(0, others(), "nothing left behind")
            assertEquals(0f, shown())
        }

    @Test
    fun aSoftClockBlendsAndEndsWhole() {
        var sharp = 0
        runTransitionTest { sharp = blendedAt(KapowEnter.clock(), 150) }
        runTransitionTest {
            val soft = blendedAt(KapowEnter.clock(softEdge = 60.dp), 150)
            assertTrue(soft > sharp + 100, "$soft against $sharp")
            advance(3000)
            assertEquals(1f, shown())
            assertEquals(0, others())
        }
    }

    @Test
    fun theClockCanTurnTheOtherWay() {
        runTransitionTest {
            set(enter = KapowEnter.clock())
            show()
            advance(100)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(240, 160), "clockwise reaches the top right first")
            assertEquals(Color.White, image.colorAt(160, 160), "and not the top left")
        }
        runTransitionTest {
            set(enter = KapowEnter.clock(counterClockwise = true))
            show()
            advance(100)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(160, 160), "counterclockwise reaches the top left first")
            assertEquals(Color.White, image.colorAt(240, 160))
            advance(3000)
            assertEquals(1f, shown())
        }
    }

    @Test
    fun aSoftWipeBlendsAndEndsWhole() {
        var sharp = 0
        runTransitionTest { sharp = blendedAt(KapowEnter.wipe(), 200) }
        runTransitionTest {
            val soft = blendedAt(KapowEnter.wipe(softEdge = 40.dp), 200)
            assertTrue(soft > sharp + 500, "$soft against $sharp")
            advance(3000)
            assertEquals(1f, shown())
            assertEquals(0, others())
        }
    }

    @Test
    fun aSoftWipeExitLeavesNothing() =
        runTransitionTest {
            hideFromShown(KapowExit.wipe(softEdge = 40.dp))
            advance(3000)
            assertEquals(0, others())
        }

    @Test
    fun theGlareHasAWidthAndAnIntensity() {
        var normal = 0
        var wide = 0
        var faint = 0
        var none = 0
        runTransitionTest {
            set(enter = KapowEnter.glare(angle = 0f, width = 10.dp))
            show()
            advance(200)
            normal = capture().lighter()
        }
        runTransitionTest {
            set(enter = KapowEnter.glare(angle = 0f, width = 60.dp))
            show()
            advance(200)
            wide = capture().lighter()
        }
        runTransitionTest {
            set(enter = KapowEnter.glare(angle = 0f, width = 60.dp, intensity = 0.4f))
            show()
            advance(200)
            faint = capture().lighter()
        }
        runTransitionTest {
            set(enter = KapowEnter.glare(angle = 0f, width = 60.dp, intensity = 0f))
            show()
            advance(200)
            none = capture().lighter()
        }
        // Without a streak only the blended column at the wipe edge is lighter than the element.
        assertTrue(none <= 100, "no intensity, no streak: $none")
        assertTrue(normal > none + 300, "a streak shows: $normal against $none")
        assertTrue(wide > normal * 2, "a wider streak lightens more: $wide against $normal")
        assertTrue(faint > none && faint < wide, "a fainter streak lightens fewer pixels: $faint, $wide")
    }

    @Test
    fun glareEndsWhole() =
        runTransitionTest {
            set(enter = KapowEnter.glare(softEdge = 20.dp, width = 30.dp, intensity = 0.5f))
            show()
            advance(3000)
            assertEquals(1f, shown())
            assertEquals(0, others())
        }

    @Test
    fun blindsCanHaveAGivenNumberOfSlats() =
        runTransitionTest {
            set(enter = KapowEnter.blinds(slatCount = 4))
            show()
            advance(150)
            val image = capture()
            val mid = shown(image)
            assertTrue(mid > 0.05f && mid < 0.95f, "$mid")
            // 4 slats of 25 px: the middle of each is open, the seam between two is not yet.
            for (y in listOf(162, 187, 212, 237)) assertEquals(Cobalt, image.colorAt(200, y), "slat at $y")
            for (y in listOf(175, 200, 225)) assertEquals(Color.White, image.colorAt(200, y), "seam at $y")
            advance(3000)
            assertEquals(1f, shown())
        }

    @Test
    fun staggeredBlindsOpenFromTheEndWhenReversed() {
        runTransitionTest {
            set(enter = KapowEnter.blinds(staggered = true, slatCount = 4))
            show()
            advance(100)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(200, 162), "the first slat opens first")
            assertEquals(Color.White, image.colorAt(200, 237), "the last one waits")
        }
        runTransitionTest {
            set(enter = KapowEnter.blinds(staggered = true, slatCount = 4, reverse = true))
            show()
            advance(100)
            val image = capture()
            assertEquals(Color.White, image.colorAt(200, 162))
            assertEquals(Cobalt, image.colorAt(200, 237), "the last slat opens first")
            advance(3000)
            assertEquals(1f, shown())
        }
    }

    @Test
    fun noStaggerAmountOpensTheSlatsTogether() {
        var together = 0f
        runTransitionTest {
            set(enter = KapowEnter.blinds(staggered = false, slatCount = 4))
            show()
            advance(100)
            together = shown()
        }
        runTransitionTest {
            set(enter = KapowEnter.blinds(staggered = true, staggerAmount = 0f, slatCount = 4))
            show()
            advance(100)
            assertEquals(together, shown())
        }
    }

    @Test
    fun theCurtainSeamCanMove() {
        runTransitionTest {
            set(enter = KapowEnter.curtain(split = 0.25f))
            show()
            advance(100)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(175, 200), "the seam is a quarter in and opens first")
            assertEquals(Color.White, image.colorAt(240, 200), "the far side is still closed")
            advance(3000)
            assertEquals(1f, shown())
        }
        runTransitionTest {
            hideFromShown(KapowExit.curtain(BlindsOrientation.Horizontal, split = 0.75f))
            advance(3000)
            assertEquals(0, others())
            assertEquals(0f, shown())
        }
    }

    @Test
    fun aShapeIrisCanTurnAndGrowPastTheNeed() {
        var upright: ImageBitmap? = null
        var plain = 0f
        runTransitionTest {
            set(enter = KapowEnter.shapeIris())
            show()
            advance(250)
            upright = capture()
            plain = shown(upright!!)
        }
        runTransitionTest {
            set(enter = KapowEnter.shapeIris(rotation = 90f))
            show()
            advance(250)
            assertNotEquals(0, capture().differsFrom(upright!!), "a turning star differs from an upright one")
            advance(3000)
            assertEquals(1f, shown(), "and still covers everything")
        }
        runTransitionTest {
            set(enter = KapowEnter.shapeIris(scale = 2f))
            show()
            advance(250)
            assertTrue(shown() > plain, "a larger one has shown more by now")
            advance(3000)
            assertEquals(1f, shown())
        }
    }

    @Test
    fun framesDoNotRecompose() =
        runTransitionTest {
            checkFramesDoNotRecompose(
                KapowEnter.clock(softEdge = 30.dp, counterClockwise = true),
                KapowExit.iris(softEdge = 30.dp),
            )
        }

    @Test
    fun reducedMotionStillFades() =
        runTransitionTest {
            checkReducedFades(
                KapowEnter.glare(softEdge = 10.dp, intensity = 0.5f),
                KapowExit.blinds(slatCount = 6, staggered = true, reverse = true),
            )
        }

    @Test
    fun keyFrames() {
        runTransitionTest { keyFrame("iris_soft_enter_150", enter = KapowEnter.iris(softEdge = 40.dp), millis = 150) }
        runTransitionTest {
            keyFrame(
                "clock_soft_ccw_enter_150",
                enter = KapowEnter.clock(softEdge = 50.dp, counterClockwise = true),
                millis = 150,
            )
        }
        runTransitionTest {
            keyFrame(
                "wipe_soft_enter_150",
                enter = KapowEnter.wipe(45f, softEdge = 40.dp),
                millis = 150,
            )
        }
        runTransitionTest {
            keyFrame("glare_wide_enter_150", enter = KapowEnter.glare(width = 40.dp, intensity = 0.7f), millis = 150)
        }
        runTransitionTest {
            keyFrame(
                "blinds_count_reverse_enter_150",
                enter = KapowEnter.blinds(staggered = true, slatCount = 6, staggerAmount = 1.5f, reverse = true),
                millis = 150,
            )
        }
        runTransitionTest {
            keyFrame(
                "curtain_split_enter_150",
                enter = KapowEnter.curtain(split = 0.3f),
                millis = 150,
            )
        }
        runTransitionTest {
            keyFrame("shape_iris_rotation_enter_150", enter = KapowEnter.shapeIris(rotation = 90f), millis = 150)
        }
    }
}
