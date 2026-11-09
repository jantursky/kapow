package com.jantursky.kapow.transitions

import kotlin.random.Random
import kotlin.time.Duration

/** No step of a flicker is shorter than this, so a second holds at most six steps: three on-off flashes. */
internal const val MIN_FLICKER_STEP_SECONDS = 1f / 6f

/**
 * Where each step of a flicker ends, as a share of the transition from 0 to 1, in order. Steps alternate between
 * off and on and the last one is on, so [flickerOn] ends in the final state. The step lengths come from [seed], so
 * they are the same on every run and target.
 *
 * At most [steps] steps are used. Steps that would be shorter than [MIN_FLICKER_STEP_SECONDS] are merged away, so
 * no second of the transition holds more than three full flashes (WCAG 2.3.1). A transition too short for two
 * steps is a single on step.
 */
internal fun flickerSteps(
    seed: Int,
    steps: Int,
    duration: Duration,
): List<Float> {
    val seconds = duration.inWholeMilliseconds / 1000f
    val room = (seconds / MIN_FLICKER_STEP_SECONDS).toInt()
    // An even count starts off and ends on.
    var count = minOf(steps, room)
    if (count % 2 == 1) count--
    if (count < 2) return listOf(1f)
    val random = Random(seed)
    val weights = List(count) { 0.5f + random.nextFloat() }
    // Every step gets the minimum; the time left over is shared out by the random weights.
    val minShare = MIN_FLICKER_STEP_SECONDS / seconds
    val spare = 1f - minShare * count
    val total = weights.sum()
    var end = 0f
    return weights.mapIndexed { index, weight ->
        end += minShare + spare * weight / total
        if (index == count - 1) 1f else end
    }
}

/** Whether a flicker with step [ends] is on at [progress], from 0 to 1. The last step is on and they alternate. */
internal fun flickerOn(
    ends: List<Float>,
    progress: Float,
): Boolean {
    val index = ends.indexOfFirst { progress < it }.let { if (it < 0) ends.lastIndex else it }
    return (ends.lastIndex - index) % 2 == 0
}
