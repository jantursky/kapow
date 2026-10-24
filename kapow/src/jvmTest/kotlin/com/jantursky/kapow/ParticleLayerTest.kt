package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.shapePainter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

/** Emits one red 40 px square that sits still at (150, 50) from the element's top left for half a second. */
private data object OneSquare : ChangeEffect() {
    override val cooldown: Duration get() = Duration.ZERO

    override fun createRunner(): ChangeRunner =
        object : ChangeRunner() {
            override fun fire(context: EffectContext) {
                val emission =
                    Emission(
                        count = 1,
                        angle = Spread(0f),
                        speed = Spread(0f),
                        lifetime = Spread(0.5f),
                        offsetX = Spread(150f),
                        offsetY = Spread(50f),
                    )
                context.emit(
                    ParticleBatch(
                        ParticleSystem(
                            emission,
                            seed = 1,
                        ),
                        shapePainter(ParticleShape.Square, listOf(Color.Red), 40f),
                        opacity = {
                            1f
                        },
                    ),
                )
            }
        }
}

@OptIn(ExperimentalTestApi::class)
class ParticleLayerTest {
    private var value by mutableIntStateOf(0)

    @Composable
    private fun Scene(
        host: Boolean,
        clip: Boolean,
        x: Int = 0,
    ) {
        val content = @Composable {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                Box(Modifier.offset(x.dp, 0.dp).size(100.dp).then(if (clip) Modifier.clipToBounds() else Modifier)) {
                    Element(Modifier.changeEffect(value, OneSquare))
                }
            }
        }
        if (host) KapowParticleLayer(Modifier.fillMaxSize()) { content() } else content()
    }

    @Test
    fun aLayerDrawsParticlesOutsideAClippingParent() =
        runEffectTest {
            setEffectContent { Scene(host = true, clip = true) }
            value++
            mainClock.advanceTimeBy(100)
            assertEquals(Color.Red, capture().colorAt(150, 50))
        }

    @Test
    fun withoutALayerAClippingParentCutsParticlesOff() =
        runEffectTest {
            setEffectContent { Scene(host = false, clip = true) }
            value++
            mainClock.advanceTimeBy(100)
            assertEquals(Color.White, capture().colorAt(150, 50))
        }

    @Test
    fun withoutALayerParticlesDrawAroundTheElement() =
        runEffectTest {
            setEffectContent { Scene(host = false, clip = false) }
            value++
            mainClock.advanceTimeBy(100)
            assertEquals(Color.Red, capture().colorAt(150, 50))
        }

    @Test
    fun particlesStartWhereTheElementIs() =
        runEffectTest {
            setEffectContent { Scene(host = true, clip = true, x = 120) }
            value++
            mainClock.advanceTimeBy(100)
            assertEquals(Color.Red, capture().colorAt(270, 50))
            assertEquals(Color.White, capture().colorAt(150, 50))
        }

    @Test
    fun particlesAreGoneAfterTheirLifetime() =
        runEffectTest {
            setEffectContent { Scene(host = true, clip = false) }
            value++
            mainClock.advanceTimeBy(700)
            assertEquals(Color.White, capture().colorAt(150, 50))
        }
}
