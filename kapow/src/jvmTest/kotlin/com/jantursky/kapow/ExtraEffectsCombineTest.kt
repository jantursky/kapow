package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class ExtraEffectsCombineTest {
    private var value by mutableIntStateOf(0)
    private var visible by mutableStateOf(false)

    @Test
    fun shakeAndShineOnOneElementBothPlayAndSettle() =
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    SolidElement(
                        Modifier
                            .offset(150.dp, 150.dp)
                            .changeEffect(value, ChangeEffect.shake(distance = 20.dp, duration = 600.milliseconds))
                            .changeEffect(value, ChangeEffect.shine(duration = 600.milliseconds)),
                    )
                }
            }
            value++
            var moved = false
            var lit = false
            repeat(25) {
                mainClock.advanceTimeBy(20)
                val image = capture()
                moved = moved || image.colorAt(152, 200) == Color.White || image.colorAt(148, 200) != Color.White
                for (x in 150..250 step 4) lit = lit || image.colorAt(x, 200).luminance > Cobalt.luminance + 0.05f
            }
            assertTrue(moved, "the shake moves the element")
            assertTrue(lit, "the shine lightens its pixels")
            mainClock.advanceTimeBy(3000)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(152, 152))
            assertEquals(Cobalt, image.colorAt(248, 248))
            assertEquals(Color.White, image.colorAt(148, 200))
            assertEquals(Color.White, image.colorAt(252, 200))
        }

    @Test
    fun shakeAndJumpAddUpAndSettle() =
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    SolidElement(
                        Modifier
                            .offset(150.dp, 150.dp)
                            .changeEffect(value, ChangeEffect.shake(distance = 20.dp))
                            .changeEffect(value, ChangeEffect.jump(height = 30.dp)),
                    )
                }
            }
            value++
            mainClock.advanceTimeBy(3000)
            val image = capture()
            assertEquals(Cobalt, image.colorAt(152, 152))
            assertEquals(Color.White, image.colorAt(148, 200))
            assertEquals(Color.White, image.colorAt(200, 148))
        }

    @Test
    fun aStaggeredListEntersOneItemAfterAnother() =
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    repeat(3) { index ->
                        KapowAnimatedVisibility(
                            visible,
                            Modifier.offset(150.dp, (20 + index * 120).dp),
                            enter = KapowEnter.boing().delay(KapowStagger.delay(index, step = 300.milliseconds)),
                            exit = KapowExit.skid().delay(KapowStagger.delay(index, step = 300.milliseconds)),
                        ) { Box(Modifier.size(100.dp).background(Cobalt)) }
                    }
                }
            }
            visible = true
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(200)
            val early = capture()
            assertNotEquals(Color.White, early.colorAt(200, 70), "the first item has started")
            assertEquals(Color.White, early.colorAt(200, 190), "the second waits")
            assertEquals(Color.White, early.colorAt(200, 310), "the third waits")
            mainClock.advanceTimeBy(3000)
            val done = capture()
            for (y in listOf(70, 190, 310)) assertEquals(Cobalt, done.colorAt(200, y), "item at $y is shown")
            visible = false
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(3000)
            val gone = capture()
            for (y in listOf(70, 190, 310)) assertEquals(Color.White, gone.colorAt(200, y), "item at $y is gone")
        }
}
