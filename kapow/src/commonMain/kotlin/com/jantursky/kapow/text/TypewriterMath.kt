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

/** Which re-roll of the scrambled glyphs [elapsedMillis] falls in. */
internal fun scrambleTick(
    elapsedMillis: Long,
    rateMillis: Long,
): Long = elapsedMillis / rateMillis

/** The random glyph shown in slot [index] during [tick]: the same inputs always give the same glyph. */
internal fun scrambleGlyph(
    index: Int,
    tick: Long,
    seed: Int,
    glyphs: String,
): Char {
    var h = seed * -1640531535 + index * 374761393 + tick.toInt() * 668265263
    h = h xor (h ushr 15)
    h *= 0x2c1b3c6d
    h = h xor (h ushr 12)
    h *= 0x297a2d39
    h = h xor (h ushr 15)
    return glyphs[(h and Int.MAX_VALUE) % glyphs.length]
}

/** Whether a slot scrambles while unresolved. Whitespace keeps showing as it is. */
internal fun isScrambled(char: Char): Boolean = !char.isWhitespace()

/** The cursor stays solid while characters appear and blinks once the text is complete. */
internal fun cursorVisible(
    elapsedMillis: Long,
    totalMillis: Long,
    blinkMillis: Long,
): Boolean = elapsedMillis < totalMillis || (elapsedMillis - totalMillis) / blinkMillis % 2 == 0L
