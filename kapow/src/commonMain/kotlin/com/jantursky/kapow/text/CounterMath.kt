package com.jantursky.kapow.text

/**
 * The value on its way from [from] to [to] at [fraction] of the way (0 to 1). It moves monotonically, and at 1 it is
 * exactly [to], whatever the rounding on the way.
 */
internal fun countValue(
    from: Long,
    to: Long,
    fraction: Float,
): Long =
    when {
        fraction >= 1f -> to
        fraction <= 0f -> from
        else -> from + ((to - from).toDouble() * fraction).toLong()
    }

/** What happens in one slot of the rolling counter. */
internal enum class SlotKind {
    /** The character stays. */
    Static,

    /** One digit rolls to another. */
    Roll,

    /** Anything else (a separator, a sign, a digit that appears or goes away) cross-fades. */
    Fade,
}

/** One right-aligned slot of the rolling counter: the character that leaves and the one that arrives. */
internal data class RollSlot(
    val from: Char?,
    val to: Char?,
) {
    val kind: SlotKind
        get() =
            when {
                from == to -> SlotKind.Static
                from != null && to != null && from.isDigit() && to.isDigit() -> SlotKind.Roll
                else -> SlotKind.Fade
            }
}

/**
 * Lines [old] and [new] up on their last characters and pairs them into slots, left to right. A slot where only
 * the longer text has a character has a null on the other side.
 */
internal fun rollSlots(
    old: String,
    new: String,
): List<RollSlot> {
    val count = maxOf(old.length, new.length)
    return List(count) { slot ->
        val fromEnd = count - 1 - slot
        RollSlot(old.getOrNull(old.length - 1 - fromEnd), new.getOrNull(new.length - 1 - fromEnd))
    }
}

/** Which way the digits move: 1 when the value goes up (the old digit leaves upward), -1 when it goes down. */
internal fun rollDirection(
    from: Long,
    to: Long,
): Int = if (to >= from) 1 else -1

/**
 * How far slot number [fromRight] (0 is the last character) has come at overall progress [t], when the slots start
 * one after the other from the right and [stagger] is the share of the duration spent on starting them. The slot
 * numbered 0 starts at once, the last one after [stagger], and every slot ends exactly at 1.
 */
internal fun slotProgress(
    t: Float,
    fromRight: Int,
    slotCount: Int,
    stagger: Float,
): Float {
    val start = if (slotCount <= 1) 0f else stagger * fromRight / (slotCount - 1)
    return ((t - start) / (1f - stagger)).coerceIn(0f, 1f)
}
