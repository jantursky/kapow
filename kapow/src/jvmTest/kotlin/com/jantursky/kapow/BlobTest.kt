package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import com.jantursky.kapow.transitions.blobPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlobTest {
    @Test
    fun entersAsAGrowingBlob() = runTransitionTest { checkEnter(KapowEnter.blob(), midMillis = 150) }

    @Test
    fun exitsAsAShrinkingBlob() = runTransitionTest { checkExit(KapowExit.blob(), midMillis = 250) }

    @Test
    fun growsFromItsOrigin() =
        runTransitionTest {
            set(enter = KapowEnter.blob(TransformOrigin(0f, 1f)))
            show()
            advance(120)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(152, 247), "the bottom left corner first")
            assertEquals(Color.White, image.colorAt(248, 152), "the opposite corner last")
        }

    @Test
    fun theOutlineIsNotACircle() {
        val bounds = blobPath(Size(100f, 100f), TransformOrigin.Center, 0.3f).getBounds()
        assertTrue(bounds.width != bounds.height, "a wobbly outline, not round: $bounds")
        assertTrue(bounds.contains(Offset(50f, 50f)))
    }

    @Test
    fun reducedMotionFades() = runTransitionTest { checkReducedFades(KapowEnter.blob(), KapowExit.blob()) }

    @Test
    fun framesDoNotRecompose() = runTransitionTest { checkFramesDoNotRecompose(KapowEnter.blob(), KapowExit.blob()) }

    @Test
    fun keyFrame() = runTransitionTest { keyFrame("blob_enter_150", enter = KapowEnter.blob(), millis = 150) }
}
