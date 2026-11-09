package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.transitions.canBlur
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GlowFromContentTest {
    private var value by mutableIntStateOf(0)

    // A round element in a 100 dp square at (150, 150). Its corner at (153, 153) is outside the circle.
    private fun ComposeUiTest.round(effect: ChangeEffect) {
        setEffectContent {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                Box(Modifier.offset(150.dp, 150.dp).size(100.dp).changeEffect(value, effect)) {
                    Box(Modifier.size(100.dp).clip(CircleShape).background(Cobalt))
                }
            }
        }
    }

    private fun ComposeUiTest.glowAt(
        x: Int,
        y: Int,
    ): Float {
        value++
        mainClock.advanceTimeBy(250)
        return 1f - capture().colorAt(x, y).green
    }

    @Test
    fun theHaloOfRoundContentKeepsTheSquaresCornerClear() =
        runEffectTest {
            round(ChangeEffect.glow(Color.Red, radius = 24.dp, fromContent = true))
            val corner = glowAt(153, 153)
            val edge = 1f - capture().colorAt(255, 200).green
            assertTrue(edge > 0.1f, "the halo shows beside the circle: $edge")
            assertTrue(corner < edge / 2, "the corner stays lighter ($corner) than the edge ($edge)")
        }

    @Test
    fun theDefaultHaloStillFollowsTheShape() =
        runEffectTest {
            round(ChangeEffect.glow(Color.Red, radius = 24.dp))
            val corner = glowAt(153, 153)
            val edge = 1f - capture().colorAt(255, 200).green
            assertTrue(corner > edge / 2, "the shape halo fills the corner ($corner) as it does the edge ($edge)")
        }

    @Test
    fun withoutBlurTheHaloFallsBackToTheShape() =
        runEffectTest {
            val before = canBlur
            canBlur = { false }
            try {
                round(ChangeEffect.glow(Color.Red, radius = 24.dp, fromContent = true))
                val corner = glowAt(153, 153)
                assertTrue(corner > 0.05f, "the stacked shape halo reaches the corner: $corner")
            } finally {
                canBlur = before
            }
        }

    @Test
    fun nothingShowsBeforeTheFirstChange() =
        runEffectTest {
            round(ChangeEffect.glow(Color.Red, radius = 24.dp, fromContent = true))
            mainClock.advanceTimeBy(500)
            val image = capture()
            assertEquals(Color.White, image.colorAt(255, 200))
            assertEquals(Color.White, image.colorAt(153, 153))
        }

    @Test
    fun theHaloFadesAway() =
        runEffectTest {
            round(ChangeEffect.glow(Color.Red, radius = 24.dp, fromContent = true))
            glowAt(255, 200)
            mainClock.advanceTimeBy(5000)
            assertEquals(Color.White, capture().colorAt(255, 200))
        }

    @Test
    fun aConditionalGlowFollowsTheContentToo() =
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    Box(
                        Modifier
                            .offset(150.dp, 150.dp)
                            .size(100.dp)
                            .conditionalEffect(
                                true,
                                ConditionalEffect.glow(Color.Red, radius = 24.dp, fromContent = true),
                            ),
                    ) {
                        Box(Modifier.size(100.dp).clip(CircleShape).background(Cobalt))
                    }
                }
            }
            mainClock.advanceTimeBy(1000)
            val image = capture()
            val corner = 1f - image.colorAt(153, 153).green
            val edge = 1f - image.colorAt(255, 200).green
            assertTrue(edge > 0.05f && corner < edge / 2, "corner $corner, edge $edge")
        }

    @Test
    fun keyFrame() =
        runEffectTest {
            round(ChangeEffect.glow(Color(0xFFFF4D6D), radius = 24.dp, fromContent = true))
            value++
            mainClock.advanceTimeBy(250)
            onRoot().captureRoboImage("src/jvmTest/screenshots/glow_from_content_250.png")
        }
}
