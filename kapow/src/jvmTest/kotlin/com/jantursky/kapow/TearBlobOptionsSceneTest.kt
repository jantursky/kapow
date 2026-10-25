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
class TearBlobOptionsSceneTest {
    private fun ImageBitmap.differsFrom(other: ImageBitmap): Int {
        val a = toPixelMap()
        val b = other.toPixelMap()
        var n = 0
        for (y in 150 until 250) for (x in 150 until 250) if (a[x, y] != b[x, y]) n++
        return n
    }

    private fun ImageBitmap.reddish(): Int {
        val pixels = toPixelMap()
        var n = 0
        for (y in 150 until 250) for (x in 150 until 250) if (pixels[x, y].red > pixels[x, y].blue + 0.2f) n++
        return n
    }

    private fun TransitionScene.enterAt(
        enter: KapowEnter,
        millis: Long,
    ): ImageBitmap {
        set(enter = enter)
        show()
        advance(millis)
        return capture()
    }

    @Test
    fun theRimTakesTheGivenColor() {
        var red = 0
        var plain = 0
        var none = 0
        runTransitionTest { red = enterAt(KapowEnter.tear(rimColor = Color.Red), 150).reddish() }
        runTransitionTest { plain = enterAt(KapowEnter.tear(), 150).reddish() }
        runTransitionTest { none = enterAt(KapowEnter.tear(rimColor = Color.Transparent), 150).reddish() }
        assertTrue(red > 50, "a red rim along the tear: $red")
        assertEquals(0, plain, "the default rim is white")
        assertEquals(0, none)
    }

    @Test
    fun noRimLeavesFewerBlendedPixels() {
        var rimmed = 0
        var bare = 0
        runTransitionTest {
            enterAt(KapowEnter.tear(), 150)
            rimmed = others()
        }
        runTransitionTest {
            enterAt(KapowEnter.tear(rimColor = Color.Transparent), 150)
            bare = others()
        }
        // Without a rim, only the antialiased edge of the tear is neither white nor cobalt.
        assertTrue(bare < rimmed, "$bare against $rimmed with the rim")
    }

    @Test
    fun roughnessChangesTheTear() {
        var smooth: ImageBitmap? = null
        var rough: ImageBitmap? = null
        runTransitionTest { smooth = enterAt(KapowEnter.tear(toothSize = 20.dp, roughness = 0f), 150) }
        runTransitionTest { rough = enterAt(KapowEnter.tear(toothSize = 20.dp, roughness = 1f), 150) }
        assertNotEquals(0, smooth!!.differsFrom(rough!!))
    }

    @Test
    fun everyTearEndsWhole() {
        for (roughness in listOf(0f, 1f)) {
            runTransitionTest {
                set(enter = KapowEnter.tear(roughness = roughness))
                show()
                advance(3000)
                assertEquals(1f, shown())
            }
            runTransitionTest { checkExit(KapowExit.tear(roughness = roughness), midMillis = 200) }
        }
    }

    @Test
    fun theBlobSeedPointsAndWobbleChangeItsShape() {
        var base: ImageBitmap? = null
        var seeded: ImageBitmap? = null
        var points: ImageBitmap? = null
        var calm: ImageBitmap? = null
        runTransitionTest { base = enterAt(KapowEnter.blob(), 150) }
        runTransitionTest { seeded = enterAt(KapowEnter.blob(seed = 3), 150) }
        runTransitionTest { points = enterAt(KapowEnter.blob(points = 4), 150) }
        runTransitionTest { calm = enterAt(KapowEnter.blob(wobble = 0f), 150) }
        assertNotEquals(0, base!!.differsFrom(seeded!!), "another seed")
        assertNotEquals(0, base!!.differsFrom(points!!), "another point count")
        assertNotEquals(0, base!!.differsFrom(calm!!), "no wobble")
    }

    @Test
    fun everyBlobCoversTheContentBeforeTheEnd() {
        for ((points, wobble) in listOf(4 to 0.5f, 5 to 0.4f, 32 to 0f)) {
            runTransitionTest {
                set(enter = KapowEnter.blob(points = points, wobble = wobble))
                show()
                advance(570)
                assertTrue(shown() > 0.98f, "$points points, wobble $wobble: ${shown()}")
            }
        }
    }

    @Test
    fun framesDoNotRecompose() =
        runTransitionTest {
            checkFramesDoNotRecompose(
                KapowEnter.tear(roughness = 0.9f, rimColor = Color.Red),
                KapowExit.blob(points = 5),
            )
        }

    @Test
    fun reducedMotionStillFades() =
        runTransitionTest { checkReducedFades(KapowEnter.tear(rimColor = Color.Red), KapowExit.blob(wobble = 0.5f)) }

    @Test
    fun keyFrames() {
        runTransitionTest {
            keyFrame(
                "tear_rough_red_enter_150",
                enter = KapowEnter.tear(45f, toothSize = 16.dp, roughness = 0.9f, rimColor = Color.Red),
                millis = 150,
            )
        }
        runTransitionTest {
            keyFrame("blob_wild_enter_150", enter = KapowEnter.blob(points = 5, wobble = 0.5f, seed = 4), millis = 150)
        }
    }
}
