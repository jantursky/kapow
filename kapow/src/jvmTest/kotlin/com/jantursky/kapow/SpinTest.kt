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
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SpinTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    private fun ComposeUiTest.scene(
        effect: ChangeEffect,
        motion: KapowMotion = KapowMotion.Full,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, effect))
            }
        }
    }

    @Test
    fun aZSpinTurnsTheElementAndSettlesUpright() =
        runEffectTest {
            scene(ChangeEffect.spin(SpinAxis.Z))
            value++
            // A turned square pokes past its upright edge in the middle of each side, except at quarter turns.
            var turned = false
            repeat(10) {
                mainClock.advanceTimeBy(50)
                turned = turned || capture().colorAt(254, 200) != Color.White
            }
            assertTrue(turned)
            mainClock.advanceTimeBy(5000)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(152, 152))
            assertEquals(Color.White, image.colorAt(254, 200))
        }

    @Test
    fun aYSpinNarrowsTheElementAsItTurns() =
        runEffectTest {
            scene(ChangeEffect.spin(SpinAxis.Y))
            value++
            mainClock.advanceTimeBy(150)
            assertEquals(Color.White, capture().colorAt(153, 200))
            mainClock.advanceTimeBy(5000)
            assertEquals(Cobalt, capture().colorAt(153, 200))
        }

    @Test
    fun aFastXSpinStillSettles() =
        runEffectTest {
            scene(ChangeEffect.spin(SpinAxis.X, SpinRate.Fast))
            value++
            var flipped = false
            repeat(10) {
                mainClock.advanceTimeBy(40)
                flipped = flipped || capture().colorAt(200, 153) == Color.White
            }
            assertTrue(flipped, "the top edge leaves its place while it turns")
            mainClock.advanceTimeBy(6000)
            assertEquals(Cobalt, capture().colorAt(200, 153))
        }

    @Test
    fun aChangeMidSpinAddsATurnAndStillSettles() =
        runEffectTest {
            scene(ChangeEffect.spin(SpinAxis.Z))
            value++
            mainClock.advanceTimeBy(200)
            value++
            mainClock.advanceTimeBy(6000)
            assertEquals(Cobalt, capture().colorAt(152, 152))
            assertEquals(Color.White, capture().colorAt(254, 200))
        }

    @Test
    fun reducedMotionFlashesInPlace() =
        runEffectTest {
            scene(ChangeEffect.spin(SpinAxis.Z), KapowMotion.Reduced)
            value++
            mainClock.advanceTimeBy(100)
            val image = capture()
            assertEquals(Color.White, image.colorAt(254, 200))
            assertTrue(image.colorAt(200, 200).luminance > Cobalt.luminance + 0.05f)
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene(ChangeEffect.spin())
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }
}
