package com.jantursky.kapow

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ModifierEqualityTest {
    @Test
    fun equalEffectsMakeEqualModifiers() {
        assertEquals(Modifier.changeEffect(1, ChangeEffect.shine()), Modifier.changeEffect(1, ChangeEffect.shine()))
        assertEquals(
            Modifier.changeEffect(1, ChangeEffect.shine()).hashCode(),
            Modifier.changeEffect(1, ChangeEffect.shine()).hashCode(),
        )
        assertNotEquals(Modifier.changeEffect(1, ChangeEffect.shine()), Modifier.changeEffect(2, ChangeEffect.shine()))
        assertEquals(
            Modifier.conditionalEffect(true, ConditionalEffect.glow()),
            Modifier.conditionalEffect(true, ConditionalEffect.glow()),
        )
        assertNotEquals(
            Modifier.conditionalEffect(true, ConditionalEffect.glow()),
            Modifier.conditionalEffect(true, ConditionalEffect.glow(Color.Red)),
        )
        val progress = { 0.5f }
        assertEquals(
            Modifier.progressEffect(ProgressEffect.shine(), progress),
            Modifier.progressEffect(ProgressEffect.shine(), progress),
        )
        assertNotEquals(
            Modifier.progressEffect(ProgressEffect.shine(), progress),
            Modifier.progressEffect(ProgressEffect.shine()) { 0.5f },
        )
    }

    @Test
    fun transitionsCompareByKindAndDuration() {
        assertEquals(KapowEnter.iris(), KapowEnter.iris())
        assertEquals(KapowEnter.iris().hashCode(), KapowEnter.iris().hashCode())
        assertNotEquals(KapowEnter.iris(), KapowEnter.clock())
        assertEquals(KapowExit.poof(), KapowExit.poof())
        assertNotEquals<Any>(KapowExit.iris(), KapowEnter.iris())
        assertEquals("KapowEnter(None, 0s)", KapowEnter.None.toString())
    }
}
