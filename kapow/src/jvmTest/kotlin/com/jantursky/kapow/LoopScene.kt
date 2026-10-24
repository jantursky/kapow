package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp

/**
 * A white window under a particle layer with the solid element at (150, 150), carrying a conditional effect while
 * [on] is true. [counter] counts how often the scene composes.
 */
@OptIn(ExperimentalTestApi::class)
class LoopScene {
    var on by mutableStateOf(false)
    val counter = CompositionCounter()

    fun show(
        test: ComposeUiTest,
        effect: ConditionalEffect,
        motion: KapowMotion = KapowMotion.Full,
    ) {
        test.setEffectContent(motion) {
            KapowParticleLayer(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    counter.Count()
                    SolidElement(Modifier.offset(150.dp, 150.dp).conditionalEffect(on, effect))
                }
            }
        }
    }

    /** Turns the effect on, steps [frames] frames and returns how often the scene composed meanwhile. */
    fun compositionsWhileOn(
        test: ComposeUiTest,
        frames: Int = 60,
    ): Int {
        test.mainClock.advanceTimeByFrame()
        val before = counter.count
        on = true
        repeat(frames) { test.mainClock.advanceTimeByFrame() }
        return counter.count - before
    }
}

/** How many pixels differ between two images of the same size. */
fun ImageBitmap.pixelsDifferentFrom(other: ImageBitmap): Int {
    val a = toPixelMap()
    val b = other.toPixelMap()
    var n = 0
    for (y in 0 until height) for (x in 0 until width) if (a[x, y] != b[x, y]) n++
    return n
}

/** Pixels in rows above [bottom] that aren't white, where only particles rising off the element can be. */
fun ImageBitmap.nonWhiteAbove(bottom: Int = 150): Int {
    val pixels = toPixelMap()
    var n = 0
    for (y in 0 until bottom) for (x in 0 until width) if (pixels[x, y] != Color.White) n++
    return n
}
