package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import com.jantursky.kapow.effects.pingPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FlashSafetyTest {
    @Test
    fun theWcagConstantIsThree() {
        assertEquals(3, KapowWcagMaxFlashesPerSecond)
    }

    @Test
    fun flashAndGlitchFlashAndWrappersAnswerForWhatTheyWrap() {
        assertTrue(ChangeEffect.flash().isFlashing)
        assertTrue(ChangeEffect.glitch().isFlashing)
        assertTrue(ChangeEffect.flash().delay(100.milliseconds).isFlashing)
        assertTrue(
            ChangeEffect
                .glitch()
                .speed(2f)
                .times(2, 50.milliseconds)
                .isFlashing,
        )
        assertTrue(
            ChangeEffect
                .flash()
                .cooldown(1.seconds)
                .reducedMotion(ReducedMotionMode.Fade)
                .isFlashing,
        )
    }

    @Test
    fun effectsThatOnlyMoveOrEmitParticlesAreNotFlashing() {
        val others =
            listOf(
                ChangeEffect.spray(),
                ChangeEffect.rise(),
                ChangeEffect.confettiPop(),
                ChangeEffect.fireworks(),
                ChangeEffect.burst(),
                ChangeEffect.pulse(),
                ChangeEffect.ripple(),
                ChangeEffect.shine(),
                ChangeEffect.spin(),
                ChangeEffect.sparkle(),
            )
        for (effect in others) assertFalse(effect.isFlashing, "$effect")
        assertFalse(ChangeEffect.spray().delay(100.milliseconds).isFlashing)
    }

    @Test
    fun aPlanUnderALimitRaisesTheCooldownAndTheGapOfAFlashingEffect() {
        val plan = ChangeEffect.flash().times(3, 10.milliseconds).plan()
        val limited = plan.limitedTo(3)
        assertEquals(1.seconds / 3, limited.cooldown)
        assertEquals(1.seconds / 3, limited.gap)
        assertEquals(1.seconds / 3 * 2, limited.gateWindow)
    }

    @Test
    fun aPlanIsLeftAloneWithoutALimitOrForANonFlashingEffect() {
        val flash = ChangeEffect.flash().plan()
        assertEquals(flash, flash.limitedTo(null))
        val spray = ChangeEffect.spray().plan()
        assertEquals(spray, spray.limitedTo(3))
    }

    @Test
    fun aSlowerCooldownOrGapIsKept() {
        val limited =
            ChangeEffect
                .flash()
                .cooldown(2.seconds)
                .times(2, 1.seconds)
                .plan()
                .limitedTo(3)
        assertEquals(2.seconds, limited.cooldown)
        assertEquals(1.seconds, limited.gap)
    }

    @Test
    fun aLimitBelowOneCountsAsOne() {
        assertEquals(1.seconds, flashInterval(0))
        assertEquals(1.seconds, flashInterval(-4))
    }

    @Test
    fun ringsAreBornAtLeastTheSpacingApart() {
        val spacing = 1.0 / 3
        // Five rings that live one second: the default spacing is 0.2 s, the limited one a third of a second.
        for (index in 0 until 5) {
            assertEquals(0f, assertNotNull(pingPhase(index * spacing, 1.0, 5, index, spacing = spacing)), 0.0001f)
        }
        // A ring that has lived its second waits for its turn instead of being born again at once.
        assertNull(pingPhase(1.2, 1.0, 5, 0, spacing = spacing))
        assertEquals(0f, assertNotNull(pingPhase(5 * spacing, 1.0, 5, 0, spacing = spacing)), 0.0001f)
    }

    @Test
    fun theDefaultSpacingIsUnchanged() {
        for (t in listOf(0.0, 0.3, 0.9, 1.0, 1.7, 3.2)) {
            for (index in 0 until 4) {
                assertEquals(
                    pingPhase(t, 1.0, 4, index),
                    pingPhase(t, 1.0, 4, index, spacing = 0.25),
                    "t=$t ring $index",
                )
            }
        }
        assertNotNull(Color.Red)
    }
}
