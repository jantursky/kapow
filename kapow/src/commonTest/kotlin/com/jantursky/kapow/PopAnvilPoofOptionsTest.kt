package com.jantursky.kapow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.transitions.MAX_POP_RINGS
import com.jantursky.kapow.transitions.popRing
import com.jantursky.kapow.transitions.popScale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PopAnvilPoofOptionsTest {
    @Test
    fun theDefaultOvershootIsTheOldPop() {
        for (t in listOf(0f, 0.05f, 0.1f, 0.2f, 0.35f, 0.6f, 1f)) {
            val x = (t / 0.35f).coerceIn(0f, 1f) - 1f
            assertEquals(1f + 2.7f * x * x * x + 1.7f * x * x, popScale(t), "at $t")
        }
    }

    @Test
    fun theOvershootScalesHowFarThePopPassesItsSize() {
        val peak = { overshoot: Float -> (0..100).maxOf { popScale(it / 100f, overshoot) } }
        assertEquals(1f, peak(0f), 1e-4f, "none")
        assertTrue(peak(1f) > 1.05f, "usual")
        assertTrue(peak(3f) > peak(1f), "more")
        for (overshoot in listOf(0f, 0.5f, 1f, 3f)) {
            assertEquals(0f, popScale(0f, overshoot), 1e-5f, "starts from nothing at $overshoot")
            assertEquals(1f, popScale(1f, overshoot), 1e-5f, "settles at $overshoot")
        }
    }

    @Test
    fun ringsFollowOneAnotherAndAreGoneByTheEnd() {
        assertEquals(0f, popRing(0, 0f))
        assertTrue(popRing(1, 0.15f) < popRing(0, 0.15f), "the second starts later")
        for (index in 0 until MAX_POP_RINGS) {
            assertTrue(popRing(index, 1f) >= 0.999f, "ring $index is gone at the end")
        }
    }

    @Test
    fun theOptionsAreChecked() {
        assertFailsWith<IllegalArgumentException> { KapowEnter.anvil(dropHeight = 0.dp) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.anvil(shake = -1f) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.anvil(dustCount = -1) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.anvil(dustCount = 41) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.pop(overshoot = -0.1f) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.pop(ringCount = 6) }
        assertFailsWith<IllegalArgumentException> { KapowEnter.pop(particleCount = 101) }
        assertFailsWith<IllegalArgumentException> { KapowExit.poof(puffCount = 0) }
        assertFailsWith<IllegalArgumentException> { KapowExit.poof(puffCount = 61) }
        assertFailsWith<IllegalArgumentException> { KapowExit.poof(puffSize = 0.dp) }
        KapowEnter.anvil(dropHeight = 1.dp, shake = 0f, dustCount = 0)
        KapowEnter.pop(overshoot = 0f, ringCount = 0, particleCount = 0)
        KapowExit.poof(puffCount = 1, puffSize = 1.dp, direction = -90f)
    }

    @Test
    fun equalOptionsMakeEqualTransitions() {
        assertEquals(KapowEnter.anvil(), KapowEnter.anvil())
        assertEquals(KapowEnter.pop(Color.Red, ringCount = 3), KapowEnter.pop(Color.Red, ringCount = 3))
        assertEquals(KapowExit.poof(direction = 10f), KapowExit.poof(direction = 10f))
        assertNotEquals(KapowEnter.anvil(), KapowEnter.anvil(shake = 0f))
        assertNotEquals(KapowEnter.anvil(), KapowEnter.anvil(dustColor = Color.Red))
        assertNotEquals(KapowEnter.pop(), KapowEnter.pop(particleCount = 0))
        assertNotEquals(KapowEnter.pop(), KapowEnter.pop(physics = ParticlePhysics.of(gravity = 400f)))
        assertNotEquals(KapowExit.poof(), KapowExit.poof(puffSize = 40.dp))
        assertNotEquals(KapowExit.poof(), KapowExit.poof(direction = 0f))
    }
}
