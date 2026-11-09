package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.internal.layerRecords
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What an element with an effect that is not drawing anything costs: no recorded layer, the same pixels. */
@OptIn(ExperimentalTestApi::class)
class IdleLayerTest {
    private var value by mutableIntStateOf(0)
    private var active by mutableStateOf(false)
    private var progress by mutableFloatStateOf(0f)

    // Read while drawing, outside the effect, so changing it redraws the element every frame.
    private var tick by mutableIntStateOf(0)

    private fun ComposeUiTest.scene(
        motion: KapowMotion = KapowMotion.Full,
        effect: Modifier.() -> Modifier,
    ) {
        setEffectContent(motion) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                SolidElement(
                    Modifier
                        .offset(150.dp, 150.dp)
                        .drawWithContent {
                            tick
                            drawContent()
                        }.effect(),
                )
            }
        }
    }

    /** Draws one more frame and returns how many layers were recorded for it. */
    private fun ComposeUiTest.frame(): Int {
        val before = layerRecords
        tick++
        mainClock.advanceTimeByFrame()
        return layerRecords - before
    }

    /** Frame by frame until the effect is idle for [QUIET] frames; returns the records of every frame. */
    private fun ComposeUiTest.playthrough(): List<Int> {
        val records = mutableListOf<Int>()
        var quiet = 0
        while (quiet < QUIET && records.size < LIMIT) {
            val n = frame()
            records += n
            quiet = if (n == 0) quiet + 1 else 0
        }
        // The frame between firing and the effect's first state change has nothing to draw yet.
        return records.dropLast(QUIET).dropWhile { it == 0 }
    }

    private fun check(
        name: String,
        records: List<Int>,
    ) {
        assertTrue(records.size >= 3, "$name plays for more than a couple of frames: $records")
        assertTrue(records.all { it == 1 }, "$name records a layer on every frame it draws: $records")
    }

    @Test
    fun aFinishedEffectRecordsNoLayerAndLooksLikeNoEffect() =
        runEffectTest {
            scene { changeEffect(value, ChangeEffect.shake(distance = 20.dp)) }
            mainClock.advanceTimeByFrame()
            val before = layerRecords
            value++
            repeat(3) { frame() }
            assertTrue(layerRecords > before, "the effect draws through a layer while it plays")
            mainClock.advanceTimeBy(3000)
            repeat(10) { assertEquals(0, frame(), "no layer at rest") }
            val image = capture()
            assertEquals(Cobalt, image.colorAt(152, 152))
            assertEquals(Cobalt, image.colorAt(248, 248))
            assertEquals(Color.White, image.colorAt(148, 200))
            assertEquals(Color.White, image.colorAt(252, 200))
        }

    @Test
    fun aRestartedEffectRecordsALayerFromItsFirstFrame() =
        runEffectTest {
            scene { changeEffect(value, ChangeEffect.shake(distance = 20.dp)) }
            value++
            mainClock.advanceTimeBy(3000)
            assertEquals(0, frame())
            value++
            mainClock.advanceTimeByFrame()
            var recorded = 0
            repeat(2) { recorded += frame() }
            assertTrue(recorded >= 1, "the restarted shake is drawn through a layer at once")
        }

    @Test
    fun everyChangeEffectDrawsThroughALayerForExactlyItsRun() {
        val full =
            mapOf(
                "shake" to ChangeEffect.shake(),
                "jump" to ChangeEffect.jump(),
                "spin" to ChangeEffect.spin(),
                "shine" to ChangeEffect.shine(),
                "stamp" to ChangeEffect.stamp(),
                "flash" to ChangeEffect.flash(),
            )
        for ((name, effect) in full) {
            runEffectTest {
                scene { changeEffect(value, effect) }
                mainClock.advanceTimeByFrame()
                value++
                mainClock.advanceTimeByFrame()
                check(name, playthrough())
            }
        }
    }

    @Test
    fun particleEffectsNeedNoLayerExceptForTheirReducedMotionFlash() {
        val effects =
            mapOf(
                "spray" to ChangeEffect.spray(),
                "sparkle" to ChangeEffect.sparkle(),
                "confettiPop" to ChangeEffect.confettiPop(),
                "fireworks" to ChangeEffect.fireworks(),
            )
        for ((name, effect) in effects) {
            runEffectTest {
                scene { changeEffect(value, effect) }
                mainClock.advanceTimeByFrame()
                value++
                mainClock.advanceTimeByFrame()
                repeat(30) { assertEquals(0, frame(), "$name draws its particles without a layer") }
            }
            runEffectTest {
                scene(KapowMotion.Reduced) { changeEffect(value, effect) }
                mainClock.advanceTimeByFrame()
                value++
                mainClock.advanceTimeByFrame()
                check("$name (reduced motion)", playthrough())
            }
        }
    }

    @Test
    fun conditionalEffectsHoldALayerOnlyWhileTheyShowAndAfterTheirFadeLeaveNone() {
        val effects =
            mapOf(
                "shimmer" to ConditionalEffect.shimmer(),
                "grayscale" to ConditionalEffect.grayscale(),
                "breathe" to ConditionalEffect.breathe(),
                "pushDown" to ConditionalEffect.pushDown(),
            )
        for ((name, effect) in effects) {
            runEffectTest {
                active = false
                scene { conditionalEffect(active, effect) }
                repeat(5) { assertEquals(0, frame(), "$name is idle before it turns on") }
                active = true
                mainClock.advanceTimeBy(1500)
                repeat(10) { assertEquals(1, frame(), "$name draws through a layer while it is on") }
                active = false
                mainClock.advanceTimeBy(3000)
                repeat(10) { assertEquals(0, frame(), "$name leaves no layer once it has faded out") }
            }
        }
    }

    @Test
    fun progressEffectsRecordALayerOnlyBetweenTheEnds() {
        val effects =
            mapOf(
                "shine" to ProgressEffect.shine(),
                "iris" to ProgressEffect.iris(),
                "fill" to ProgressEffect.fill(),
            )
        for ((name, effect) in effects) {
            runEffectTest {
                scene { progressEffect(effect) { progress } }
                progress = 0f
                repeat(3) { assertEquals(0, frame(), "$name at 0") }
                progress = 0.5f
                mainClock.advanceTimeByFrame()
                repeat(5) { assertEquals(1, frame(), "$name at 0.5") }
                progress = 1f
                mainClock.advanceTimeByFrame()
                if (name != "fill") repeat(3) { assertEquals(0, frame(), "$name at 1") }
            }
        }
    }

    private companion object {
        const val QUIET = 8
        const val LIMIT = 600
    }
}
