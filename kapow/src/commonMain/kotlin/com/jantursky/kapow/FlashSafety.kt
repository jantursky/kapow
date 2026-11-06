package com.jantursky.kapow

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * The most flashes per second that Kapow effects below this point may make, or null for no limit (the default).
 *
 * With a limit of N, the flashing change effects (`flash`, `glitch`, and `repeat` of one of them) ignore a trigger
 * that comes less than 1/N seconds after the last one that played, and never queue it. A repeating effect starts
 * its plays at least 1/N seconds apart, `ping` starts its rings at least 1/N seconds apart, and `glitch` changes
 * its look at most N times a second. Effects that only move or emit particles are not limited. A value below 1 is
 * treated as 1.
 *
 * This is a cap on the rate, not a test of WCAG 2.3.1: it does not measure brightness change or the area of the
 * flash, so it is not a claim of conformance. Use [KapowWcagMaxFlashesPerSecond] for the three flashes per second
 * that guideline names.
 *
 * ```
 * CompositionLocalProvider(LocalKapowMaxFlashesPerSecond provides KapowWcagMaxFlashesPerSecond) { App() }
 * ```
 */
public val LocalKapowMaxFlashesPerSecond: ProvidableCompositionLocal<Int?> = compositionLocalOf { null }

/** The three flashes per second that WCAG 2.3.1 (Three Flashes or Below Threshold) names as the limit. */
@Suppress("ktlint:standard:property-naming")
public const val KapowWcagMaxFlashesPerSecond: Int = 3
