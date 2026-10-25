package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ShatterDisintegrateSceneTest {
    private fun exitImage(
        exit: KapowExit,
        millis: Long,
    ): ImageBitmap {
        var image: ImageBitmap? = null
        runTransitionTest {
            hideFromShown(exit)
            advance(millis)
            image = capture()
        }
        return image!!
    }

    private fun ImageBitmap.differsFrom(other: ImageBitmap): Int {
        val a = toPixelMap()
        val b = other.toPixelMap()
        var n = 0
        for (y in 0 until 400) for (x in 0 until 400) if (a[x, y] != b[x, y]) n++
        return n
    }

    private fun ImageBitmap.inked(
        xs: IntRange = 0 until 400,
        ys: IntRange = 0 until 400,
    ): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in ys) for (x in xs) if (pixels[x, y] != Color.White) n++
        return n
    }

    /** The mean column of the pixels in [ys] that are not white. */
    private fun ImageBitmap.meanX(ys: IntRange = 0 until 400): Float {
        val pixels = toPixelMap()
        var sum = 0L
        var n = 0
        for (y in ys) {
            for (x in 0 until 400) {
                if (pixels[x, y] != Color.White) {
                    sum += x
                    n++
                }
            }
        }
        return if (n == 0) 0f else sum.toFloat() / n
    }

    @Test
    fun theFallFollowsGravity() {
        val below = 252 until 400
        val none = exitImage(KapowExit.shatter(gravity = 0f), 500).inked(ys = below)
        val usual = exitImage(KapowExit.shatter(), 500).inked(ys = below)
        val heavy = exitImage(KapowExit.shatter(gravity = 3f), 500).inked(ys = below)
        assertTrue(none < usual, "no gravity, nothing falls: $none against $usual")
        assertTrue(heavy > usual, "heavier falls further: $heavy against $usual")
    }

    @Test
    fun spinAndSeedChangeTheFrame() {
        val usual = exitImage(KapowExit.shatter(), 400)
        assertNotEquals(0, usual.differsFrom(exitImage(KapowExit.shatter(spin = 0f), 400)), "no spin")
        assertNotEquals(0, usual.differsFrom(exitImage(KapowExit.shatter(spin = 3f), 400)), "more spin")
        assertNotEquals(0, usual.differsFrom(exitImage(KapowExit.shatter(seed = 5), 400)), "another seed")
        assertEquals(0, usual.differsFrom(exitImage(KapowExit.shatter(), 400)), "the same every time")
    }

    @Test
    fun thePiecesLeanAwayFromTheImpact() {
        val left = exitImage(KapowExit.shatter(shards = 60, impact = TransformOrigin(0f, 0.5f)), 600).meanX()
        val right = exitImage(KapowExit.shatter(shards = 60, impact = TransformOrigin(1f, 0.5f)), 600).meanX()
        assertTrue(left > right, "struck on the left they end up further right: $left against $right")
    }

    @Test
    fun everyShatterVariantEndsClean() {
        runTransitionTest {
            checkExit(
                KapowExit.shatter(shards = 50, gravity = 2f, spin = 2f, impact = TransformOrigin(0f, 0f), seed = 9),
                midMillis = 250,
            )
        }
        runTransitionTest {
            hideFromShown(KapowExit.shatter(gravity = 0f, spin = 0f))
            advance(3000)
            assertEquals(0f, shown())
            assertEquals(0, others(), "nothing left behind")
        }
    }

    @Test
    fun largerGrainsMakeMoreDust() {
        val above = 0 until 148
        val fine = exitImage(KapowExit.disintegrate(particleSize = 2.dp), 700).inked(ys = above)
        val coarse = exitImage(KapowExit.disintegrate(particleSize = 12.dp), 700).inked(ys = above)
        assertTrue(coarse > fine, "$coarse px against $fine px")
    }

    @Test
    fun theWindBlowsTheDust() {
        val above = 0 until 148
        val left = exitImage(KapowExit.disintegrate(wind = -1f), 700).meanX(above)
        val calm = exitImage(KapowExit.disintegrate(), 700).meanX(above)
        val right = exitImage(KapowExit.disintegrate(wind = 1f), 700).meanX(above)
        assertTrue(left < calm && calm < right, "$left, $calm, $right")
    }

    @Test
    fun everyDisintegrateVariantEndsClean() {
        for ((size, wind) in listOf(1.dp to -1f, 16.dp to 1f)) {
            runTransitionTest {
                hideFromShown(KapowExit.disintegrate(particleSize = size, wind = wind))
                advance(500)
                val mid = shown()
                assertTrue(mid > 0.05f && mid < 0.95f, "part of the element shows mid-way, not $mid")
                advance(4000)
                assertEquals(0f, shown())
                assertEquals(0, others(), "nothing left behind")
            }
        }
    }

    @Test
    fun framesDoNotRecompose() =
        runTransitionTest {
            checkFramesDoNotRecompose(
                KapowEnter.None,
                KapowExit.shatter(gravity = 2f, impact = TransformOrigin(1f, 0f)),
            )
        }

    @Test
    fun disintegrateFramesDoNotRecompose() =
        runTransitionTest {
            checkFramesDoNotRecompose(KapowEnter.None, KapowExit.disintegrate(particleSize = 6.dp, wind = 0.5f))
        }

    @Test
    fun reducedMotionStillFades() {
        runTransitionTest { checkReducedExitFades(KapowExit.shatter(gravity = 3f, spin = 3f, seed = 2)) }
        runTransitionTest { checkReducedExitFades(KapowExit.disintegrate(particleSize = 8.dp, wind = 1f)) }
    }

    @Test
    fun keyFrames() {
        runTransitionTest {
            keyFrame(
                "shatter_corner_heavy_exit_400",
                exit =
                    KapowExit.shatter(
                        shards = 40,
                        gravity = 2f,
                        spin = 3f,
                        impact = TransformOrigin(0f, 0f),
                        seed = 7,
                    ),
                millis = 400,
            )
        }
        runTransitionTest {
            keyFrame(
                "disintegrate_wind_coarse_exit_600",
                exit = KapowExit.disintegrate(particleSize = 8.dp, wind = 1f),
                millis = 600,
            )
        }
    }
}
