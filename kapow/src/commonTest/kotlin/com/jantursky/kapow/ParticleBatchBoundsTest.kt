package com.jantursky.kapow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.painterOf
import com.jantursky.kapow.particles.shapePainter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParticleBatchBoundsTest {
    private fun batch(
        count: Int = 30,
        area: Float = 200f,
        size: Float = 20f,
        speed: Float = 0f,
    ): ParticleBatch =
        ParticleBatch(
            ParticleSystem(
                Emission(
                    count = count,
                    angle = Spread(0f, 360f),
                    speed = Spread(speed),
                    lifetime = Spread(2f),
                    offsetX = Spread(0f, area),
                    offsetY = Spread(0f, area),
                ),
                seed = 7,
            ),
            shapePainter(ParticleShape.Circle, listOf(Color.Red), size),
            opacity = { 1f },
        )

    @Test
    fun theBoundsCoverEveryParticleAndItsSize() {
        val batch = batch()
        batch.step(0.1f)
        val bounds = assertNotNull(batch.liveBounds())
        // Particles start inside a 200 px square; each is 20 px across, so it reaches 10 px past its center.
        assertTrue(bounds.left <= -10f && bounds.top <= -10f, "$bounds")
        assertTrue(bounds.right >= 210f - 20f && bounds.bottom >= 210f - 20f, "$bounds")
    }

    @Test
    fun theBoundsStayNearTheEmissionArea() {
        val batch = batch()
        batch.step(0.1f)
        val bounds = assertNotNull(batch.liveBounds())
        assertTrue(bounds.width < 1000f && bounds.height < 1000f, "$bounds")
        assertTrue(bounds.width < 260f && bounds.height < 260f, "a 200 px area plus the particle size: $bounds")
    }

    @Test
    fun theBoundsFollowTheOrigin() {
        val batch = batch()
        batch.step(0.1f)
        val before = assertNotNull(batch.liveBounds())
        batch.origin =
            androidx.compose.ui.geometry
                .Offset(300f, -50f)
        val after = assertNotNull(batch.liveBounds())
        assertEquals(before.left + 300f, after.left, 0.001f)
        assertEquals(before.top - 50f, after.top, 0.001f)
    }

    @Test
    fun aBatchWithNothingAliveHasNoBounds() {
        val batch = batch()
        repeat(60) { batch.step(0.1f) }
        assertNull(batch.liveBounds())
    }

    @Test
    fun aPainterThatReachesFurtherWidensTheBounds() {
        val small = batch()
        val wide =
            ParticleBatch(
                ParticleSystem(
                    Emission(1, Spread(0f), Spread(0f), Spread(2f)),
                    seed = 1,
                ),
                painterOf(60f) { _, _ -> },
                opacity = { 1f },
            )
        wide.step(0.1f)
        small.step(0.1f)
        val bounds = assertNotNull(wide.liveBounds())
        assertTrue(bounds.width >= 120f, "$bounds")
    }

    @Test
    fun aFollowedOriginMovesUntilItIsFrozenOrTheEmitterIsGone() {
        val batch = batch()
        var emitter: Offset? = Offset(10f, 20f)
        batch.follow = { emitter }
        batch.followEmitter()
        assertEquals(Offset(10f, 20f), batch.origin)
        emitter = Offset(50f, 60f)
        batch.followEmitter()
        assertEquals(Offset(50f, 60f), batch.origin)
        emitter = null
        batch.followEmitter()
        assertEquals(Offset(50f, 60f), batch.origin, "keeps the last origin without an emitter")
        emitter = Offset(1f, 1f)
        batch.freeze()
        batch.followEmitter()
        assertEquals(Offset(50f, 60f), batch.origin, "a frozen batch stays put")
    }
}
