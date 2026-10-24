package com.jantursky.kapow.particles

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.LayoutCoordinates

/** A particle layer that draws batches for elements below it. */
internal interface ParticleHost {
    /** Takes [batch], moving its origin from [emitter]'s coordinates into the layer's. Returns false if it can't. */
    fun add(
        batch: ParticleBatch,
        emitter: LayoutCoordinates,
    ): Boolean
}

internal val LocalParticleHost = staticCompositionLocalOf<ParticleHost?> { null }
