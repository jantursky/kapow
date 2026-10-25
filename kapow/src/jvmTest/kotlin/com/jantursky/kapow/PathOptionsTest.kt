package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import com.jantursky.kapow.effects.taperedLine
import com.jantursky.kapow.effects.wavyPath
import kotlin.test.Test
import kotlin.test.assertEquals

class PathOptionsTest {
    @Test
    fun aTaperedLineNarrowsToItsHead() {
        val flat = taperedLine(Offset(0f, 0f), Offset(10f, 0f), 4f, 0f)
        val pointed = taperedLine(Offset(0f, 0f), Offset(10f, 0f), 4f, 1f)
        assertEquals(4f, flat.getBounds().height)
        assertEquals(4f, pointed.getBounds().height)
        assertEquals(10f, pointed.getBounds().right)
    }

    @Test
    fun aWavyLineSwingsWithinItsAmplitudeAndEndsWhereTheLineDoes() {
        val path = wavyPath(Offset(0f, 10f), Offset(100f, 10f), amplitude = 4f, length = 20f)
        val bounds = path.getBounds()
        assertEquals(10f - 4f, bounds.top, 0.2f)
        assertEquals(10f + 4f, bounds.bottom, 0.2f)
        assertEquals(100f, bounds.right, 1e-3f)
        assertEquals(0f, wavyPath(Offset(5f, 5f), Offset(5f, 5f), 4f, 20f).getBounds().width)
    }
}
