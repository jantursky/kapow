package com.jantursky.kapow

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.Halo
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GlowTest {
    private var value by mutableIntStateOf(0)
    private val counter = CompositionCounter()

    // A point 8 px right of the element, inside a 16 dp halo.
    private val haloX = 258
    private val haloY = 200

    private fun ComposeUiTest.scene(
        motion: KapowMotion = KapowMotion.Full,
        modifier: () -> Modifier,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(Modifier.offset(150.dp, 150.dp).then(modifier()))
            }
        }
    }

    @Test
    fun aChangeFlaresAHaloThatFades() =
        runEffectTest {
            scene { Modifier.changeEffect(value, ChangeEffect.glow(Color.Red)) }
            value++
            mainClock.advanceTimeBy(250)
            val halo = capture().colorAt(haloX, haloY)
            assertTrue(halo.red > halo.blue + 0.05f, "reddish halo: $halo")
            mainClock.advanceTimeBy(4000)
            assertEquals(Color.White, capture().colorAt(haloX, haloY))
        }

    @Test
    fun theContentBrightensWhileItGlows() =
        runEffectTest {
            scene { Modifier.changeEffect(value, ChangeEffect.glow(Color.Red)) }
            val base = capture().colorAt(200, 200).luminance
            value++
            mainClock.advanceTimeBy(250)
            assertTrue(capture().colorAt(200, 200).luminance > base + 0.02f)
        }

    @Test
    fun repeatedChangesGlowStronger() =
        runEffectTest {
            scene { Modifier.changeEffect(value, ChangeEffect.glow(Color.Red)) }
            value++
            mainClock.advanceTimeBy(250)
            val once = 1f - capture().colorAt(haloX + 6, haloY).green
            mainClock.advanceTimeBy(4000)
            repeat(3) {
                value++
                mainClock.advanceTimeBy(50)
            }
            mainClock.advanceTimeBy(100)
            val thrice = 1f - capture().colorAt(haloX + 6, haloY).green
            assertTrue(thrice > once, "three quick changes ($thrice) beat one ($once)")
        }

    @Test
    fun reducedMotionGivesAFaintShortHalo() =
        runEffectTest {
            scene(KapowMotion.Reduced) { Modifier.changeEffect(value, ChangeEffect.glow(Color.Red)) }
            value++
            mainClock.advanceTimeBy(400)
            assertEquals(Color.White, capture().colorAt(haloX, haloY))
        }

    @Test
    fun aConditionalGlowStaysWhileTheConditionHolds() =
        runEffectTest {
            var on by mutableStateOf(true)
            scene { Modifier.conditionalEffect(on, ConditionalEffect.glow(Color.Red)) }
            mainClock.advanceTimeBy(2000)
            assertNotEquals(Color.White, capture().colorAt(haloX, haloY))
            on = false
            mainClock.advanceTimeBy(400)
            assertEquals(Color.White, capture().colorAt(haloX, haloY))
        }

    @Test
    fun aProgressGlowFollowsTheProgress() =
        runEffectTest {
            var progress by mutableFloatStateOf(0f)
            scene { Modifier.progressEffect(ProgressEffect.glow(Color.Red)) { progress } }
            mainClock.advanceTimeByFrame()
            assertEquals(Color.White, capture().colorAt(haloX, haloY))
            val before = counter.count
            progress = 1f
            mainClock.advanceTimeByFrame()
            assertNotEquals(Color.White, capture().colorAt(haloX, haloY))
            assertEquals(before, counter.count, "progress changes redraw without recomposition")
        }

    @Test
    fun aProgressLambdaThatCapturesAPlainValueStillUpdates() =
        runEffectTest {
            var progress by mutableFloatStateOf(0f)
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    // Read here, so the lambda below captures a plain Float, as a parameter would be.
                    val value = progress
                    SolidElement(
                        Modifier.offset(150.dp, 150.dp).progressEffect(ProgressEffect.glow(Color.Red)) { value },
                    )
                }
            }
            mainClock.advanceTimeByFrame()
            assertEquals(Color.White, capture().colorAt(haloX, haloY))
            progress = 1f
            mainClock.advanceTimeByFrame()
            assertNotEquals(Color.White, capture().colorAt(haloX, haloY))
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene { Modifier.changeEffect(value, ChangeEffect.glow()) }
            mainClock.advanceTimeByFrame()
            val before = counter.count
            value++
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1)
        }

    @Test
    fun blurredAndStackedHalos() =
        runEffectTest {
            setEffectContent {
                Row(Modifier.fillMaxSize().background(Color.White).padding(40.dp)) {
                    for (blur in listOf(true, false)) {
                        val graphics = LocalGraphicsContext.current
                        val halo =
                            remember {
                                Halo(
                                    Color(0xFF6B62F2),
                                    16.dp,
                                    RoundedCornerShape(16.dp),
                                ) { blur }.also { it.attach(graphics) }
                            }
                        Canvas(Modifier.padding(20.dp).size(100.dp)) {
                            halo.draw(this, 1f)
                            drawRoundRect(
                                Cobalt,
                                cornerRadius =
                                    androidx.compose.ui.geometry
                                        .CornerRadius(16.dp.toPx()),
                            )
                        }
                    }
                }
            }
            onRoot().captureRoboImage("src/jvmTest/screenshots/glow_blur_and_fallback.png")
        }
}
