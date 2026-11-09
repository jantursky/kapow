package com.jantursky.kapow

import com.jantursky.kapow.internal.requireArg
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Delays that grow with an item's position, so the items of a list start their effects and transitions one after
 * another. Pass the result to `ChangeEffect.delay` or to [KapowEnter.delay] and [KapowExit.delay].
 *
 * ```
 * items.forEachIndexed { index, item ->
 *     Row(Modifier.changeEffect(count, ChangeEffect.shine().delay(KapowStagger.delay(index))))
 * }
 * ```
 */
public object KapowStagger {
    /**
     * The delay of the item at [index]: [index] times [step], never above [max].
     *
     * @param index the item's position in the list, 0 or more. Item 0 starts at once.
     * @param step how much later each item starts than the one before, 0 or more. 50 ms by default.
     * @param max the longest delay, so long lists do not wait for ever. 500 ms by default.
     */
    public fun delay(
        index: Int,
        step: Duration = 50.milliseconds,
        max: Duration = 500.milliseconds,
    ): Duration {
        requireArg(index >= 0, "index", "not be negative", index)
        requireArg(!step.isNegative(), "step", "not be negative", step)
        requireArg(!max.isNegative(), "max", "not be negative", max)
        return minOf(step * index, max)
    }
}
