package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CheckerTest {
    @Test
    fun entersInTwoWaves() = runTransitionTest { checkEnter(KapowEnter.checker(cellSize = 20.dp), midMillis = 250) }

    @Test
    fun exitsInTwoWaves() = runTransitionTest { checkExit(KapowExit.checker(cellSize = 20.dp), midMillis = 200) }

    @Test
    fun oneColorOpensBeforeTheOther() =
        runTransitionTest {
            set(enter = KapowEnter.checker(cellSize = 20.dp))
            show()
            advance(170)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(160, 160), "the middle of the first square")
            assertEquals(Color.White, image.colorAt(151, 151), "its corner")
            assertEquals(Color.White, image.colorAt(180, 160), "the middle of its neighbour, in the second wave")
            assertEquals(Cobalt, image.colorAt(180, 180), "the middle of the square diagonal to it")
        }

    @Test
    fun reducedMotionFades() = runTransitionTest { checkReducedFades(KapowEnter.checker(), KapowExit.checker()) }

    @Test
    fun framesDoNotRecompose() =
        runTransitionTest { checkFramesDoNotRecompose(KapowEnter.checker(), KapowExit.checker()) }

    @Test
    fun rejectsTinyCells() {
        assertFailsWith<IllegalArgumentException> { KapowEnter.checker(cellSize = 1.dp) }
        assertFailsWith<IllegalArgumentException> { KapowExit.checker(cellSize = 0.dp) }
    }

    @Test
    fun keyFrame() =
        runTransitionTest { keyFrame("checker_enter_250", enter = KapowEnter.checker(cellSize = 20.dp), millis = 250) }
}
