package com.jantursky.kapow

import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParticleSystemTest {
    private val emission =
        Emission(
            count = 8,
            angle = Spread(-120f, -60f),
            speed = Spread(100f, 200f),
            lifetime = Spread(1f),
            gravity = 400f,
            drag = 0.5f,
            spin = Spread(-90f, 90f),
        )

    @Test
    fun theSameSeedAndFramesGiveTheSameParticles() {
        val a = ParticleSystem(emission, seed = 7)
        val b = ParticleSystem(emission, seed = 7)
        repeat(20) {
            a.step(1f / 60)
            b.step(1f / 60)
        }
        assertContentEquals(a.x, b.x)
        assertContentEquals(a.y, b.y)
        assertContentEquals(a.rotation, b.rotation)
    }

    @Test
    fun aDifferentSeedScattersDifferently() {
        val a = ParticleSystem(emission, seed = 1).also { it.step(0.1f) }
        val b = ParticleSystem(emission, seed = 2).also { it.step(0.1f) }
        assertFalse(a.x.contentEquals(b.x))
    }

    @Test
    fun particlesLaunchWithinTheirAngle() {
        val system = ParticleSystem(emission, seed = 3)
        system.step(0.01f)
        for (i in 0 until system.count) assertTrue(system.y[i] < 0f, "upward angles move up")
    }

    @Test
    fun gravityPullsParticlesBackDown() {
        val system = ParticleSystem(emission.copy(drag = 1f, gravity = 2000f), seed = 3)
        repeat(30) { system.step(1f / 30) }
        assertTrue((0 until system.count).all { system.y[it] > 0f })
    }

    @Test
    fun particlesExpireAfterTheirLifetime() {
        val system = ParticleSystem(emission, seed = 3)
        assertTrue(system.step(0.5f))
        assertTrue(system.isAlive(0))
        assertFalse(system.step(0.6f))
        assertFalse(system.isAlive(0))
    }

    @Test
    fun progressRunsFromZeroToOne() {
        val system = ParticleSystem(emission, seed = 3)
        assertEquals(0f, system.progress(0))
        system.step(0.25f)
        assertTrue(abs(system.progress(0) - 0.25f) < 1e-4f)
    }

    @Test
    fun staggeredParticlesStartOneAfterAnother() {
        val system = ParticleSystem(emission.copy(stagger = 0.7f), seed = 3)
        system.step(0.05f)
        assertTrue(system.isAlive(0))
        assertFalse(system.isAlive(system.count - 1))
        system.step(0.7f)
        assertTrue(system.isAlive(system.count - 1))
        assertTrue(system.step(0.9f), "the last particle still has time left")
    }

    @Test
    fun aSpreadOfOneValueAlwaysPicksIt() {
        assertEquals(3f, Spread(3f).pick(kotlin.random.Random(1)))
    }
}
