package com.jantursky.kapow.multihaptic

import com.jantursky.kapow.KapowFeedbackEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HapticMappingTest {
    private val builtIn =
        listOf(
            KapowFeedbackEvent.Selection,
            KapowFeedbackEvent.ImpactLight,
            KapowFeedbackEvent.ImpactMedium,
            KapowFeedbackEvent.ImpactHeavy,
            KapowFeedbackEvent.Success,
            KapowFeedbackEvent.Warning,
            KapowFeedbackEvent.Error,
        )

    @Test
    fun everyBuiltInEventVibrates() {
        for (event in builtIn) assertNotNull(hapticEffectFor(event), "$event")
    }

    @Test
    fun eventsFeelDifferent() {
        val effects = builtIn.map { hapticEffectFor(it)!!.primitives }
        assertEquals(effects.size, effects.toSet().size)
    }

    @Test
    fun notificationsAreSequences() {
        assertEquals(1, hapticEffectFor(KapowFeedbackEvent.ImpactHeavy)!!.primitives.size)
        assertEquals(2, hapticEffectFor(KapowFeedbackEvent.Success)!!.primitives.size)
        assertEquals(3, hapticEffectFor(KapowFeedbackEvent.Error)!!.primitives.size)
    }

    @Test
    fun customEventsAreLeftToTheApp() {
        assertNull(hapticEffectFor(KapowFeedbackEvent.Custom("confetti")))
    }
}
