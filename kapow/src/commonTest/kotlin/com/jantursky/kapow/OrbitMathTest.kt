package com.jantursky.kapow

import androidx.compose.ui.unit.dp
import com.jantursky.kapow.effects.orbitAlpha
import com.jantursky.kapow.effects.orbitDepth
import com.jantursky.kapow.effects.orbitPosition
import com.jantursky.kapow.effects.orbitScale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration

class OrbitMathTest {
    private fun near(
        a: Float,
        b: Float,
    ) = abs(a - b) < 1e-4f

    @Test
    fun anUntiltedOrbitIsAnEllipse() {
        val right = orbitPosition(0f, 50f, 10f, 0f)
        assertTrue(near(right.x, 50f) && near(right.y, 0f))
        val bottom = orbitPosition(PI.toFloat() / 2, 50f, 10f, 0f)
        assertTrue(near(bottom.x, 0f) && near(bottom.y, 10f))
    }

    @Test
    fun aTiltTurnsTheEllipse() {
        val p = orbitPosition(0f, 50f, 10f, PI.toFloat() / 2)
        assertTrue(near(p.x, 0f) && near(p.y, 50f))
    }

    @Test
    fun theNearHalfIsBiggerAndBrighter() {
        assertTrue(orbitDepth(PI.toFloat() / 2) > 0f)
        assertTrue(orbitDepth(-PI.toFloat() / 2) < 0f)
        assertEquals(1f, orbitScale(1f))
        assertTrue(orbitScale(-1f) < orbitScale(0f))
        assertEquals(1f, orbitAlpha(1f))
        assertTrue(orbitAlpha(-1f) < orbitAlpha(0f))
    }

    @Test
    fun theFactoryChecksItsArguments() {
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.orbit(emptyList()) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.orbit(count = 0) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.orbit(size = 0.dp) }
        assertFailsWith<IllegalArgumentException> { ConditionalEffect.orbit(duration = Duration.ZERO) }
    }
}
