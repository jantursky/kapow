package com.jantursky.kapow.samples.web

import kotlin.time.Duration

/**
 * What an example does on its own while autoplay runs: [step] plays the effect the way the main control would
 * (fire it, toggle the condition, show or hide, sweep the progress), then autoplay waits [every] before the next
 * step, long enough for the effect to finish. [every] is asked again before each wait, so it can follow the
 * state the last step left behind (a condition stays on longer than off) or the page's duration slider.
 *
 * The first step comes right after the page opens, unless [startsOn]: the page opens with its condition already
 * holding, so the first step waits a full [every].
 */
internal class Autoplay(
    val every: () -> Duration,
    val startsOn: Boolean = false,
    val step: suspend () -> Unit,
) {
    constructor(every: Duration, step: suspend () -> Unit) : this({ every }, step = step)
}
