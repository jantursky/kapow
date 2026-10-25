package com.jantursky.kapow.text

/** Characters after which the typewriter holds for the extra pause. */
private const val PAUSE_PUNCTUATION = ",.;:!?…"

internal fun isPausePunctuation(char: Char): Boolean = char in PAUSE_PUNCTUATION

/**
 * Milliseconds after the start at which the character at [index] has appeared. Every character takes
 * `1000 / charsPerSecond`, and each punctuation mark before it adds [pauseMillis].
 */
internal fun appearMillis(
    text: String,
    index: Int,
    charsPerSecond: Float,
    pauseMillis: Long,
): Long {
    var pauses = 0
    for (k in 0 until index) if (isPausePunctuation(text[k])) pauses++
    return ((index + 1) * 1000.0 / charsPerSecond).toLong() + pauses * pauseMillis
}

/** How many characters of [text] are visible [elapsedMillis] after the reveal was asked for. */
internal fun visibleCount(
    text: String,
    elapsedMillis: Long,
    charsPerSecond: Float,
    startDelayMillis: Long,
    pauseMillis: Long,
): Int {
    val t = elapsedMillis - startDelayMillis
    if (t < 0) return 0
    var count = 0
    var pauses = 0
    for (k in text.indices) {
        val at = ((k + 1) * 1000.0 / charsPerSecond).toLong() + pauses * pauseMillis
        if (at > t) break
        count = k + 1
        if (isPausePunctuation(text[k])) pauses++
    }
    return count
}

/** When the last character of [text] appears, counted from the request, including [startDelayMillis]. */
internal fun totalMillis(
    text: String,
    charsPerSecond: Float,
    startDelayMillis: Long,
    pauseMillis: Long,
): Long =
    if (text.isEmpty()) {
        startDelayMillis
    } else {
        startDelayMillis + appearMillis(text, text.lastIndex, charsPerSecond, pauseMillis)
    }

/** The cursor stays solid while characters appear and blinks once the text is complete. */
internal fun cursorVisible(
    elapsedMillis: Long,
    totalMillis: Long,
    blinkMillis: Long,
): Boolean = elapsedMillis < totalMillis || (elapsedMillis - totalMillis) / blinkMillis % 2 == 0L
