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
