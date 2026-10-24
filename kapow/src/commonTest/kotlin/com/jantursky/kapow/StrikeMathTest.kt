package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kapow.effects.strikeLine
import kotlin.test.Test
import kotlin.test.assertEquals

class StrikeMathTest {
    private val size = Size(200f, 40f)

    @Test
    fun theLineRunsThroughTheMiddleFromTheLeft() {
        assertEquals(Offset(0f, 20f) to Offset(0f, 20f), strikeLine(size, 0f, LayoutDirection.Ltr))
        assertEquals(Offset(0f, 20f) to Offset(100f, 20f), strikeLine(size, 0.5f, LayoutDirection.Ltr))
        assertEquals(Offset(0f, 20f) to Offset(200f, 20f), strikeLine(size, 1.5f, LayoutDirection.Ltr))
    }

    @Test
    fun rightToLeftLayoutsStrikeFromTheRight() {
        assertEquals(Offset(200f, 20f) to Offset(150f, 20f), strikeLine(size, 0.25f, LayoutDirection.Rtl))
        assertEquals(Offset(200f, 20f) to Offset(0f, 20f), strikeLine(size, 1f, LayoutDirection.Rtl))
    }
}
