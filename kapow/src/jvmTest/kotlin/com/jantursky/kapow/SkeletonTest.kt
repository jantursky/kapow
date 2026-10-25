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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class SkeletonTest {
    private var loading by mutableStateOf(true)
    private val counter = CompositionCounter()
    private val gray = Color(0xFF808080)

    private fun androidx.compose.ui.test.ComposeUiTest.scene(
        motion: KapowMotion = KapowMotion.Full,
        shimmer: Boolean = true,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                counter.Count()
                SolidElement(
                    Modifier
                        .offset(150.dp, 150.dp)
                        .kapowSkeleton(loading, RectangleShape, gray, shimmer = shimmer),
                )
            }
        }
    }

    @Test
    fun theContentIsCoveredWhileLoadingAndShownAfter() =
        runEffectTest {
            scene(shimmer = false)
            mainClock.advanceTimeBy(100)
            assertEquals(gray, capture().colorAt(200, 200))
            loading = false
            mainClock.advanceTimeBy(600)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun theRevealFadesThePlaceholderAway() =
        runEffectTest {
            scene(shimmer = false)
            mainClock.advanceTimeBy(100)
            loading = false
            mainClock.advanceTimeBy(150)
            val middle = capture().colorAt(200, 200)
            assertTrue(middle != gray && middle != Cobalt, "between the placeholder and the content: $middle")
        }

    @Test
    fun sizeAndPositionDoNotChange() =
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    SolidElement(Modifier.offset(150.dp, 150.dp).kapowSkeleton(loading))
                }
            }
            mainClock.advanceTimeBy(100)
            val before = onNodeWithTag("element").getUnclippedBoundsInRoot()
            loading = false
            mainClock.advanceTimeBy(600)
            assertEquals(before, onNodeWithTag("element").getUnclippedBoundsInRoot())
        }

    @Test
    fun startingLoadedShowsTheContentAtOnce() =
        runEffectTest {
            loading = false
            scene()
            mainClock.advanceTimeByFrame()
            assertEquals(Cobalt, capture().colorAt(200, 200))
            val first = capture()
            mainClock.advanceTimeBy(700)
            assertEquals(0, capture().pixelsDifferentFrom(first))
        }

    @Test
    fun theBandSweepsThePlaceholder() =
        runEffectTest {
            scene()
            mainClock.advanceTimeBy(300)
            val early = capture()
            mainClock.advanceTimeBy(450)
            val later = capture()
            assertTrue(early.pixelsDifferentFrom(later) > 0, "the band moved")
            assertTrue(later.colorAt(190, 200).luminance > gray.luminance + 0.02f, "a band of light")
        }

    @Test
    fun reducedMotionKeepsThePlaceholderStillAndCrossFades() =
        runEffectTest {
            scene(KapowMotion.Reduced)
            mainClock.advanceTimeBy(100)
            val first = capture()
            assertEquals(gray, first.colorAt(200, 200))
            mainClock.advanceTimeBy(900)
            assertEquals(0, capture().pixelsDifferentFrom(first), "no shimmer")
            loading = false
            mainClock.advanceTimeBy(150)
            val middle = capture().colorAt(200, 200)
            assertTrue(middle != gray && middle != Cobalt, "a cross-fade: $middle")
            mainClock.advanceTimeBy(600)
            assertEquals(Cobalt, capture().colorAt(200, 200))
        }

    @Test
    fun framesDoNotRecompose() =
        runEffectTest {
            scene()
            mainClock.advanceTimeByFrame()
            val before = counter.count
            repeat(60) { mainClock.advanceTimeByFrame() }
            loading = false
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertTrue(counter.count - before <= 1, "recomposed ${counter.count - before} times")
        }

    @Test
    fun parametersAreChecked() {
        assertFailsWith<IllegalArgumentException> { Modifier.kapowSkeleton(true, duration = 0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { Modifier.kapowSkeleton(true, revealDuration = (-1).milliseconds) }
    }

    @Test
    fun keyFrame() =
        runEffectTest {
            scene()
            mainClock.advanceTimeBy(700)
            onRoot().captureRoboImage("src/jvmTest/screenshots/skeleton_700.png")
        }
}
