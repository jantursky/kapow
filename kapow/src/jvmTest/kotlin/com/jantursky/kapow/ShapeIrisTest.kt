package com.jantursky.kapow

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.transitions.MIN_RATIO
import com.jantursky.kapow.transitions.outlineRatio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ShapeIrisTest {
    private val density = Density(1f)

    private fun ratio(
        shape: Shape,
        side: Float = 200f,
    ) = outlineRatio(shape.createOutline(Size(side, side), LayoutDirection.Ltr, density), side)

    @Test
    fun entersAsAGrowingStar() = runTransitionTest { checkEnter(KapowEnter.shapeIris(), midMillis = 200) }

    @Test
    fun exitsAsAShrinkingStar() = runTransitionTest { checkExit(KapowExit.shapeIris(), midMillis = 300) }

    @Test
    fun theStarShowsItsTipsFirst() =
        runTransitionTest {
            set(enter = KapowEnter.shapeIris(StarShape()))
            show()
            advance(150)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(200, 200), "the middle")
            // The top tip reaches farther up than the notch between the two upper tips reaches up and to the side.
            val up = (150 until 200).first { image.colorAt(200, it) == Cobalt }
            val notch = (0 until 50).first { image.colorAt(200 + it / 2, 200 - it) != Cobalt }
            assertTrue(200 - up > notch, "a tip at ${200 - up} px, the notch at $notch px")
        }

    @Test
    fun aCircleFromTheCornerMatchesTheIris() =
        runTransitionTest {
            set(enter = KapowEnter.shapeIris(CircleShape, TransformOrigin(0f, 0f)))
            show()
            advance(100)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(152, 152))
            assertEquals(Color.White, image.colorAt(248, 248))
        }

    @Test
    fun reducedMotionFades() = runTransitionTest { checkReducedFades(KapowEnter.shapeIris(), KapowExit.shapeIris()) }

    @Test
    fun framesDoNotRecompose() =
        runTransitionTest { checkFramesDoNotRecompose(KapowEnter.shapeIris(), KapowExit.shapeIris()) }

    @Test
    fun shapesHoldTheExpectedCircle() {
        assertEquals(0.5f, ratio(RectangleShape))
        assertEquals(0.5f, ratio(RoundedCornerShape(16.dp)))
        assertEquals(0.5f, ratio(CircleShape))
        assertEquals(0.25f, ratio(StarShape(5, 0.5f)), 0.01f)
        assertTrue(ratio(StarShape(8, 0.8f)) > ratio(StarShape(8, 0.4f)))
        // A shape away from its own center would never cover; it stops growing at the floor.
        val corner = GenericShape { size, _ -> addRect(Rect(0f, 0f, size.width / 4, size.height / 4)) }
        assertEquals(MIN_RATIO, ratio(corner))
    }

    @Test
    fun theStarIsAValueAndChecksItsArguments() {
        assertEquals(StarShape(6, 0.4f), StarShape(6, 0.4f))
        assertEquals(StarShape().hashCode(), StarShape().hashCode())
        assertNotEquals(StarShape(5), StarShape(6))
        assertEquals("StarShape(points=5, innerRatio=0.5)", StarShape().toString())
        assertTrue(StarShape().createOutline(Size(10f, 10f), LayoutDirection.Ltr, density) is Outline.Generic)
        assertFailsWith<IllegalArgumentException> { StarShape(points = 2) }
        assertFailsWith<IllegalArgumentException> { StarShape(innerRatio = 0f) }
        assertFailsWith<IllegalArgumentException> { StarShape(innerRatio = 1.5f) }
    }

    @Test
    fun keyFrame() =
        runTransitionTest { keyFrame("shape_iris_enter_150", enter = KapowEnter.shapeIris(), millis = 150) }
}
