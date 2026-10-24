package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class CurtainTest {
    @Test
    fun entersFromTheMiddle() = runTransitionTest { checkEnter(KapowEnter.curtain(), midMillis = 100) }

    @Test
    fun exitsIntoTheMiddle() = runTransitionTest { checkExit(KapowExit.curtain(), midMillis = 250) }

    @Test
    fun aVerticalSeamPartsLeftAndRight() =
        runTransitionTest {
            set(enter = KapowEnter.curtain(BlindsOrientation.Vertical))
            show()
            advance(100)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(200, 152), "the middle opens first")
            assertEquals(Color.White, image.colorAt(152, 200), "the left half still covers its edge")
            assertEquals(Color.White, image.colorAt(248, 200), "and so does the right")
        }

    @Test
    fun aHorizontalSeamPartsUpAndDown() =
        runTransitionTest {
            set(enter = KapowEnter.curtain(BlindsOrientation.Horizontal))
            show()
            advance(100)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(152, 200))
            assertEquals(Color.White, image.colorAt(200, 152))
            assertEquals(Color.White, image.colorAt(200, 248))
        }

    @Test
    fun reducedMotionFades() = runTransitionTest { checkReducedFades(KapowEnter.curtain(), KapowExit.curtain()) }

    @Test
    fun framesDoNotRecompose() =
        runTransitionTest { checkFramesDoNotRecompose(KapowEnter.curtain(), KapowExit.curtain()) }

    @Test
    fun keyFrame() = runTransitionTest { keyFrame("curtain_enter_150", enter = KapowEnter.curtain(), millis = 150) }
}
