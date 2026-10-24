package com.jantursky.kapow.platform

private fun prefersReducedMotion(): Boolean =
    js("typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches")

internal actual fun platformReducedMotion(): Boolean = prefersReducedMotion()
