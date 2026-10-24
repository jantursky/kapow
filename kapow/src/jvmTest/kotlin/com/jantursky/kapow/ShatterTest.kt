package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ShatterTest {
    @Test
    fun exitsInPieces() = runTransitionTest { checkExit(KapowExit.shatter(), midMillis = 400) }

    @Test
    fun crackLinesOpenFirst() =
        runTransitionTest {
            hideFromShown(KapowExit.shatter())
            advance(100)
            val image = capture()
            val shown = shown(image)
            assertTrue(shown > 0.8f && shown < 1f, "cracks between whole pieces, $shown shown")
        }

    @Test
    fun thePiecesFallBelowTheElement() =
        runTransitionTest {
            hideFromShown(KapowExit.shatter())
            advance(500)
            val pixels = capture().toPixelMap()
            var below = 0
            for (y in 252 until 400) for (x in 100 until 300) if (pixels[x, y] != Color.White) below++
            assertTrue(below > 100, "pieces below the element, $below px")
        }

    @Test
    fun reducedMotionFades() = runTransitionTest { checkReducedExitFades(KapowExit.shatter()) }

    @Test
    fun framesDoNotRecompose() = runTransitionTest { checkFramesDoNotRecompose(KapowEnter.None, KapowExit.shatter()) }

    @Test
    fun rejectsAShardCountOutOfRange() {
        assertFailsWith<IllegalArgumentException> { KapowExit.shatter(shards = 1) }
        assertFailsWith<IllegalArgumentException> { KapowExit.shatter(shards = 201) }
    }

    @Test
    fun keyFrame() = runTransitionTest { keyFrame("shatter_exit_400", exit = KapowExit.shatter(), millis = 400) }
}
