package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ParticleContentTest {
    private var value by mutableIntStateOf(0)

    /** Sprays [content] once and counts the red pixels above the element shortly after. */
    private fun redAbove(content: ParticleContent): Int {
        var n = 0
        runEffectTest {
            setEffectContent {
                KapowParticleLayer(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(Color.White)) {
                        SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, ChangeEffect.spray(content)))
                    }
                }
            }
            value++
            mainClock.advanceTimeBy(200)
            val pixels = capture().toPixelMap()
            for (y in 0 until 150) {
                for (x in 0 until 400) {
                    val c = pixels[x, y]
                    if (c.red > 0.6f && c.green < 0.5f && c.blue < 0.5f) n++
                }
            }
        }
        return n
    }

    @Test
    fun everyBuiltInShapeDraws() {
        for (shape in ParticleShape.entries) {
            assertTrue(redAbove(ParticleContent.shape(shape, listOf(Color.Red), 12.dp)) > 0, "$shape")
        }
    }

    @Test
    fun aPainterDrawsTintedOrNot() {
        assertTrue(redAbove(ParticleContent.painter(ColorPainter(Color.Red), 12.dp)) > 0)
        assertTrue(redAbove(ParticleContent.painter(ColorPainter(Color.Black), 12.dp, tint = Color.Red)) > 0)
    }

    @Test
    fun textDraws() {
        assertTrue(redAbove(ParticleContent.text("+1", TextStyle(color = Color.Red, fontSize = 20.sp))) > 0)
    }

    @Test
    fun shapesNeedAColor() {
        assertFailsWith<IllegalArgumentException> { ParticleContent.shape(colors = emptyList()) }
    }
}
