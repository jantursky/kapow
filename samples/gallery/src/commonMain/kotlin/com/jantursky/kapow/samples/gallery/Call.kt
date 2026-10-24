package com.jantursky.kapow.samples.gallery

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import kotlin.time.Duration

private const val LINE_WIDTH = 80
private const val INDENT = 4

/**
 * A call to show as source: its [name] and arguments, each a ready-made `String` or a nested [Call]. An argument
 * with an empty name is positional. [render] puts it on one line when it fits and breaks it into one argument per
 * line otherwise, the way the code would be formatted.
 */
internal class Call(
    private val name: String,
    private val args: List<Pair<String, Any>>,
) {
    fun render(indent: Int = 0): String {
        val flat = flat()
        if (args.isEmpty() || indent + flat.length <= LINE_WIDTH) return flat
        val pad = " ".repeat(indent + INDENT)
        val lines =
            args.joinToString("") { (key, value) ->
                val label = label(key)
                "$pad$label${text(value, indent + INDENT + label.length)},\n"
            }
        return "$name(\n$lines${" ".repeat(indent)})"
    }

    private fun flat(): String = "$name(${args.joinToString { (key, value) -> "${label(key)}${flat(value)}" }})"

    private fun label(key: String) = if (key.isEmpty()) "" else "$key = "

    private fun flat(value: Any) = if (value is Call) value.flat() else value.toString()

    private fun text(
        value: Any,
        indent: Int,
    ) = if (value is Call) value.render(indent) else value.toString()

    override fun toString(): String = render()
}

/** A call named [name] with [args] as `"name" to value`, or `"" to value` for a positional one. */
internal fun call(
    name: String,
    vararg args: Pair<String, Any>,
): Call = Call(name, args.toList())

/** `Modifier.changeEffect(count, effect)`, with the value as the page holds it. */
internal fun changeEffectCode(
    effect: Call,
    value: String = "count",
): String = call("Modifier.changeEffect", "" to value, "" to effect).render()

/** `Modifier.conditionalEffect(isActive, effect)`. */
internal fun conditionalEffectCode(
    effect: Call,
    condition: String = "isActive",
): String = call("Modifier.conditionalEffect", "" to condition, "" to effect).render()

/** `Modifier.progressEffect(effect) { progress }`. */
internal fun progressEffectCode(
    effect: Call,
    progress: String = "progress",
): String = "${call("Modifier.progressEffect", "" to effect).render()} { $progress }"

/** A `KapowAnimatedVisibility` with the [enter] and [exit] the page has picked; either may be left out. */
internal fun transitionCode(
    enter: Call? = null,
    exit: Call? = null,
    visible: String = "visible",
): String {
    val args = listOfNotNull("" to visible, enter?.let { "enter" to it }, exit?.let { "exit" to it })
    return "${Call("KapowAnimatedVisibility", args).render()} {\n    // your content\n}"
}

/** `45f`, `0.5f`, `-12.25f`: a float as a source literal, at most two decimals. */
internal fun Float.kt(): String {
    val hundredths = (this * 100).roundToInt()
    val whole = hundredths / 100
    val fraction = kotlin.math.abs(hundredths % 100)
    val sign = if (hundredths < 0 && whole == 0) "-" else ""
    return when {
        fraction == 0 -> "${sign}${whole}f"
        fraction % 10 == 0 -> "${sign}$whole.${fraction / 10}f"
        else -> "$sign$whole.${fraction.toString().padStart(2, '0')}f"
    }
}

/** `3.dp`, `12.5.dp`. */
internal fun Dp.kt(): String = "${value.kt().removeSuffix("f").removeSuffix(".0")}.dp"

/** `2.seconds` for whole seconds, `450.milliseconds` otherwise. */
internal fun Duration.kt(): String {
    val millis = inWholeMilliseconds
    return if (millis >= 1000 && millis % 1000 == 0L) "${millis / 1000}.seconds" else "$millis.milliseconds"
}

/** `Color(0xFF6B62F2)`. */
internal fun Color.kt(): String = "Color(0x${toArgb().toUInt().toString(16).uppercase().padStart(8, '0')})"
