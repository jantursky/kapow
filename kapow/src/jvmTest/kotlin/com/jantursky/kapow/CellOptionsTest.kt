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
class CellOptionsTest {
    private fun ImageBitmap.differsFrom(other: ImageBitmap): Int {
        val a = toPixelMap()
        val b = other.toPixelMap()
        var n = 0
        for (y in 150 until 250) for (x in 150 until 250) if (a[x, y] != b[x, y]) n++
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
    fun everyCellShapeEntersAndExits() {
        for (shape in CellShape.entries) {
            runTransitionTest { checkEnter(KapowEnter.dissolve(cellSize = 10.dp, cellShape = shape), midMillis = 200) }
            runTransitionTest { checkExit(KapowExit.dissolve(cellSize = 10.dp, cellShape = shape), midMillis = 200) }
        }
    }

    @Test
    fun softCellsCoverTheContentJustBeforeTheEnd() {
        for (shape in CellShape.entries) {
            runTransitionTest {
                set(enter = KapowEnter.dissolve(cellSize = 10.dp, cellShape = shape, softness = 0.5f))
                show()
                advance(570)
                val share = shown()
                assertTrue(share > 0.985f, "$shape: nearly all of it shows, $share")
            }
        }
    }

    @Test
    fun theShapesLookDifferent() {
        val images = mutableListOf<ImageBitmap>()
        for (shape in CellShape.entries) {
            runTransitionTest { images += enterAt(KapowEnter.dissolve(cellSize = 20.dp, cellShape = shape), 200) }
        }
        assertNotEquals(0, images[0].differsFrom(images[1]), "squares and circles")
        assertNotEquals(0, images[0].differsFrom(images[2]), "squares and hexagons")
        assertNotEquals(0, images[1].differsFrom(images[2]), "circles and hexagons")
    }

    @Test
    fun theSeedSetsTheOrder() {
        var first: ImageBitmap? = null
        var again: ImageBitmap? = null
        var other: ImageBitmap? = null
        runTransitionTest { first = enterAt(KapowEnter.dissolve(cellSize = 10.dp, seed = 7), 200) }
        runTransitionTest { again = enterAt(KapowEnter.dissolve(cellSize = 10.dp, seed = 7), 200) }
        runTransitionTest { other = enterAt(KapowEnter.dissolve(cellSize = 10.dp, seed = 8), 200) }
        assertEquals(0, first!!.differsFrom(again!!), "the same seed, the same order")
        assertNotEquals(0, first!!.differsFrom(other!!), "another seed, another order")
    }

    @Test
    fun softnessGrowsCellsFromTheirMiddle() =
        runTransitionTest {
            set(enter = KapowEnter.dissolve(cellSize = 20.dp, softness = 1f))
            show()
            advance(250)
            val image = capture()
            // With every cell growing together, cell centers show before cell corners do.
            var middles = 0
            var corners = 0
            for (row in 0 until 5) {
                for (column in 0 until 5) {
                    val x = 150 + column * 20
                    val y = 150 + row * 20
                    if (image.colorAt(x + 10, y + 10) == Cobalt) middles++
                    if (image.colorAt(x + 1, y + 1) == Cobalt) corners++
                }
            }
            assertTrue(middles > corners, "$middles middles against $corners corners")
            assertTrue(middles > 0)
        }

    @Test
    fun everyCheckerOrderEntersAndExits() {
        for (order in CheckerOrder.entries) {
            try {
                runTransitionTest { checkEnter(KapowEnter.checker(cellSize = 20.dp, order = order), midMillis = 350) }
                runTransitionTest { checkExit(KapowExit.checker(cellSize = 20.dp, order = order), midMillis = 220) }
            } catch (e: AssertionError) {
                throw AssertionError("$order: ${e.message}", e)
            }
        }
    }

    @Test
    fun aDiagonalCheckerStartsInTheFirstCorner() =
        runTransitionTest {
            set(enter = KapowEnter.checker(cellSize = 25.dp, order = CheckerOrder.Diagonal))
            show()
            advance(120)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(162, 162), "the first cell is open")
            assertEquals(Color.White, image.colorAt(237, 237), "the last one has not started")
        }

    @Test
    fun aSpiralCheckerStartsInTheMiddle() =
        runTransitionTest {
            set(enter = KapowEnter.checker(cellSize = 25.dp, order = CheckerOrder.Spiral))
            show()
            // The middle cells open first; look at the moment the first of them has opened a little.
            var image = capture()
            var frames = 0
            while (image.colorAt(187, 187) != Cobalt && frames++ < 60) {
                advance(16)
                image = capture()
            }
            assertEquals(Cobalt, image.colorAt(187, 187), "the middle has started")
            assertEquals(Color.White, image.colorAt(162, 162), "the corner waits")
            assertEquals(Color.White, image.colorAt(237, 237))
        }

    @Test
    fun theCheckerSeedSetsTheRandomOrder() {
        var first: ImageBitmap? = null
        var again: ImageBitmap? = null
        var other: ImageBitmap? = null
        val random = { seed: Int -> KapowEnter.checker(cellSize = 20.dp, order = CheckerOrder.Random, seed = seed) }
        runTransitionTest { first = enterAt(random(1), 200) }
        runTransitionTest { again = enterAt(random(1), 200) }
        runTransitionTest { other = enterAt(random(2), 200) }
        assertEquals(0, first!!.differsFrom(again!!))
        assertNotEquals(0, first!!.differsFrom(other!!))
    }

    @Test
    fun framesDoNotRecompose() =
        runTransitionTest {
            checkFramesDoNotRecompose(
                KapowEnter.dissolve(cellShape = CellShape.Hexagon, softness = 0.4f),
                KapowExit.checker(order = CheckerOrder.Spiral),
            )
        }

    @Test
    fun reducedMotionStillFades() =
        runTransitionTest {
            checkReducedFades(
                KapowEnter.dissolve(cellShape = CellShape.Circle),
                KapowExit.checker(order = CheckerOrder.Diagonal),
            )
        }

    @Test
    fun keyFrames() {
        runTransitionTest {
            keyFrame(
                "dissolve_hex_enter_200",
                enter = KapowEnter.dissolve(cellSize = 14.dp, cellShape = CellShape.Hexagon),
                millis = 200,
            )
        }
        runTransitionTest {
            keyFrame(
                "dissolve_circle_soft_enter_200",
                enter = KapowEnter.dissolve(cellSize = 14.dp, cellShape = CellShape.Circle, softness = 0.6f),
                millis = 200,
            )
        }
        runTransitionTest {
            keyFrame(
                "checker_spiral_enter_250",
                enter = KapowEnter.checker(cellSize = 16.dp, order = CheckerOrder.Spiral),
                millis = 250,
            )
        }
    }
}
