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
class PopAnvilPoofSceneTest {
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

    private fun ImageBitmap.reddish(): Int = count { it.red > it.blue + 0.2f }

    private fun ImageBitmap.inked(
        xs: IntRange,
        ys: IntRange,
    ): Int = count(xs, ys) { it != Color.White }

    private fun ImageBitmap.differsFrom(other: ImageBitmap): Int {
        val a = toPixelMap()
        val b = other.toPixelMap()
        var n = 0
        for (y in 0 until 400) for (x in 0 until 400) if (a[x, y] != b[x, y]) n++
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

    private fun TransitionScene.exitAt(
        exit: KapowExit,
        millis: Long,
    ): ImageBitmap {
        hideFromShown(exit)
        advance(millis)
        return capture()
    }

    private fun enterImage(
        enter: KapowEnter,
        millis: Long,
    ): ImageBitmap {
        var image: ImageBitmap? = null
        runTransitionTest { image = enterAt(enter, millis) }
        return image!!
    }

    private fun exitImage(
        exit: KapowExit,
        millis: Long,
    ): ImageBitmap {
        var image: ImageBitmap? = null
        runTransitionTest { image = exitAt(exit, millis) }
        return image!!
    }

    @Test
    fun aLowerDropStartsCloserToItsPlace() {
        val low = enterImage(KapowEnter.anvil(dropHeight = 20.dp), 50)
        val usual = enterImage(KapowEnter.anvil(), 50)
        runTransitionTest {
            assertTrue(shown(low) > 0.3f, "the low drop is already in view")
            assertEquals(0f, shown(usual), "the usual drop is still above the window")
        }
    }

    @Test
    fun withoutAShakeTheAnvilStaysPutAfterLanding() {
        var calm = 1f
        var shaken = 1f
        runTransitionTest {
            set(enter = KapowEnter.anvil(shake = 0f))
            show()
            advance(400)
            repeat(25) {
                calm = minOf(calm, shown())
                advance(40)
            }
        }
        runTransitionTest {
            set(enter = KapowEnter.anvil())
            show()
            advance(400)
            repeat(25) {
                shaken = minOf(shaken, shown())
                advance(40)
            }
        }
        assertEquals(1f, calm, "no shake")
        assertTrue(shaken < 1f, "the usual shake moves it: $shaken")
    }

    @Test
    fun theDustTakesItsColorAndCount() {
        val side = (0 until 140) + (260 until 400)
        val plain = enterImage(KapowEnter.anvil(), 300)
        val red = enterImage(KapowEnter.anvil(dustColor = Color.Red), 300)
        val none = enterImage(KapowEnter.anvil(dustCount = 0), 300)
        val many = enterImage(KapowEnter.anvil(dustCount = 40), 300)
        assertEquals(0, plain.reddish(), "grey dust")
        assertTrue(red.reddish() > 20, "red dust: ${red.reddish()}")
        assertEquals(0, none.inked(0 until 140, 200 until 270) + none.inked(260 until 400, 200 until 270), "no dust")
        assertTrue(many.differsFrom(plain) > 0 && side.isNotEmpty(), "more puffs")
        assertTrue(
            many.inked(0 until 140, 200 until 270) > plain.inked(0 until 140, 200 until 270),
            "more dust beside the element",
        )
    }

    /** The most red pixels any frame of [enter] shows, looking every 40 ms. */
    private fun mostRed(enter: KapowEnter): Int {
        var most = 0
        runTransitionTest {
            set(enter = enter)
            show()
            repeat(20) {
                advance(40)
                most = maxOf(most, capture().count { it.red > it.blue + 0.05f })
            }
        }
        return most
    }

    @Test
    fun theRingCountAndColorShapeThePop() {
        val none = mostRed(KapowEnter.pop(Color.Red, ringCount = 0, particleCount = 0))
        val one = mostRed(KapowEnter.pop(Color.Red, ringCount = 1, particleCount = 0))
        val three = mostRed(KapowEnter.pop(Color.Red, ringCount = 3, particleCount = 0))
        assertEquals(0, none, "no ring and no burst")
        assertTrue(one > 50, "a red ring: $one")
        assertTrue(three > one, "$three against $one")
    }

    @Test
    fun theBurstTakesItsParticleCountAndPhysics() {
        val none = enterImage(KapowEnter.pop(Color.Red, ringCount = 0, particleCount = 0), 300)
        val burst = enterImage(KapowEnter.pop(Color.Red, ringCount = 0, particleCount = 100), 300)
        val heavy =
            enterImage(
                KapowEnter.pop(
                    Color.Red,
                    ringCount = 0,
                    particleCount = 100,
                    physics = ParticlePhysics.of(gravity = 900f),
                ),
                300,
            )
        assertEquals(0, none.reddish(), "no burst")
        assertTrue(burst.reddish() > 20, "particles fly: ${burst.reddish()}")
        assertNotEquals(0, burst.differsFrom(heavy), "gravity bends the burst")
    }

    @Test
    fun theOvershootChangesHowFarThePopGrows() {
        val calm = enterImage(KapowEnter.pop(overshoot = 0f), 200)
        val big = enterImage(KapowEnter.pop(overshoot = 3f), 200)
        assertNotEquals(0, calm.differsFrom(big))
    }

    @Test
    fun thePuffsTakeTheirCountSizeAndDirection() {
        val few = exitImage(KapowExit.poof(Color.Black, puffCount = 1), 50)
        val many = exitImage(KapowExit.poof(Color.Black, puffCount = 60), 50)
        val small = exitImage(KapowExit.poof(Color.Black, puffCount = 20, puffSize = 6.dp), 50)
        val large = exitImage(KapowExit.poof(Color.Black, puffCount = 20, puffSize = 60.dp), 50)
        val up = exitImage(KapowExit.poof(Color.Black, puffCount = 30, direction = -90f), 80)
        val down = exitImage(KapowExit.poof(Color.Black, puffCount = 30, direction = 90f), 80)
        val all = 0 until 400
        assertTrue(many.inked(all, all) > few.inked(all, all), "more puffs")
        assertTrue(large.inked(all, all) > small.inked(all, all), "bigger puffs")
        val aboveUp = up.inked(all, 0 until 200)
        val belowUp = up.inked(all, 200 until 400)
        val aboveDown = down.inked(all, 0 until 200)
        val belowDown = down.inked(all, 200 until 400)
        assertTrue(aboveUp > belowUp, "drifts up: $aboveUp against $belowUp")
        assertTrue(belowDown > aboveDown, "drifts down: $belowDown against $aboveDown")
    }

    @Test
    fun everyVariantStillFinishesCleanly() {
        runTransitionTest {
            checkEnter(KapowEnter.anvil(dropHeight = 60.dp, shake = 2f, dustCount = 0), midMillis = 30)
        }
        runTransitionTest {
            set(enter = KapowEnter.pop(ringCount = 5, overshoot = 3f, particleCount = 100))
            show()
            advance(3000)
            assertEquals(1f, shown())
            assertEquals(0, others(), "nothing left behind")
        }
        runTransitionTest {
            hideFromShown(KapowExit.poof(puffCount = 60, puffSize = 60.dp, direction = 0f))
            advance(3000)
            assertEquals(0f, shown())
            assertEquals(0, others(), "nothing left behind")
        }
    }

    @Test
    fun framesDoNotRecompose() =
        runTransitionTest {
            checkFramesDoNotRecompose(
                KapowEnter.pop(Color.Red, ringCount = 3, overshoot = 2f),
                KapowExit.poof(puffCount = 40, direction = -90f),
            )
        }

    @Test
    fun anvilFramesDoNotRecompose() =
        runTransitionTest {
            checkFramesDoNotRecompose(KapowEnter.anvil(dropHeight = 80.dp, dustColor = Color.Red), KapowExit.None)
        }

    @Test
    fun reducedMotionStillFades() =
        runTransitionTest {
            checkReducedFades(KapowEnter.pop(ringCount = 3, particleCount = 50), KapowExit.poof(puffCount = 40))
        }

    @Test
    fun anvilReducedMotionStillFades() =
        runTransitionTest {
            set(enter = KapowEnter.anvil(dustColor = Color.Red, dustCount = 20), motion = KapowMotion.Reduced)
            show()
            advance(150)
            val image = capture()
            for ((x, y) in listOf(151 to 151, 200 to 200, 248 to 248)) {
                assertNotEquals(Color.White, image.colorAt(x, y), "($x, $y) shows without a drop")
                assertNotEquals(Cobalt, image.colorAt(x, y), "($x, $y) is faded")
            }
            assertEquals(100 * 100, others(image), "no dust")
            advance(3000)
            assertEquals(1f, shown())
        }

    @Test
    fun keyFrames() {
        runTransitionTest {
            keyFrame(
                "anvil_red_dust_enter_300",
                enter = KapowEnter.anvil(dropHeight = 120.dp, dustColor = Color.Red, dustCount = 20),
                millis = 300,
            )
        }
        runTransitionTest {
            keyFrame(
                "pop_rings_burst_enter_300",
                enter = KapowEnter.pop(Color.Red, ringCount = 3, particleCount = 60, overshoot = 2f),
                millis = 300,
            )
        }
        runTransitionTest {
            keyFrame(
                "poof_up_exit_100",
                exit = KapowExit.poof(Color.Black, puffCount = 30, puffSize = 40.dp, direction = -90f),
                millis = 100,
            )
        }
    }
}
