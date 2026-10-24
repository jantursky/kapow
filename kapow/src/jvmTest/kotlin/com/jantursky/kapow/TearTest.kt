package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TearTest {
    @Test
    fun entersBehindATornEdge() = runTransitionTest { checkEnter(KapowEnter.tear(), midMillis = 150) }

    @Test
    fun exitsBehindATornEdge() = runTransitionTest { checkExit(KapowExit.tear(), midMillis = 250) }

    @Test
    fun theEdgeMovesAlongItsAngleAndIsJagged() =
        runTransitionTest {
            set(enter = KapowEnter.tear(angle = 90f, toothSize = 16.dp))
            show()
            advance(150)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(200, 152), "the top shows first when it moves down")
            assertEquals(Color.White, image.colorAt(200, 248))
            // The edge is where each column stops showing; on a tear it is not a straight line.
            val edges =
                (152 until 248 step 4).map { x ->
                    (150 until 250).first { image.colorAt(x, it) == Color.White }
                }
            assertTrue(edges.max() - edges.min() >= 6, "edge rows $edges")
        }

    @Test
    fun theRimIsLighterThanTheContent() =
        runTransitionTest {
            set(enter = KapowEnter.tear(angle = 0f))
            show()
            advance(150)
            val image = capture()
            val brightest = (150 until 250).maxOf { image.colorAt(it, 200).luminance }
            assertTrue(brightest > Cobalt.luminance + 0.1f)
        }

    @Test
    fun reducedMotionFades() = runTransitionTest { checkReducedFades(KapowEnter.tear(), KapowExit.tear()) }

    @Test
    fun framesDoNotRecompose() = runTransitionTest { checkFramesDoNotRecompose(KapowEnter.tear(), KapowExit.tear()) }

    @Test
    fun rejectsTinyTeeth() {
        assertFailsWith<IllegalArgumentException> { KapowEnter.tear(toothSize = 1.dp) }
        assertFailsWith<IllegalArgumentException> { KapowExit.tear(toothSize = 1.dp) }
    }

    @Test
    fun keyFrame() = runTransitionTest { keyFrame("tear_enter_150", enter = KapowEnter.tear(), millis = 150) }
}
