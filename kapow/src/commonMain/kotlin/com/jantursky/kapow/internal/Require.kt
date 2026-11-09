package com.jantursky.kapow.internal

/**
 * Throws an [IllegalArgumentException] naming the argument, the rule it broke and the value it had, such as
 * `count must be positive, was 0`, unless [condition] holds.
 */
internal inline fun requireArg(
    condition: Boolean,
    name: String,
    rule: String,
    value: Any?,
) {
    if (!condition) throw IllegalArgumentException("$name must $rule, was $value")
}
