package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jantursky.kapow.CounterMode
import com.jantursky.kapow.KapowCounter
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.time.Duration

/** A score that moves to its new value. The first value shows at once; each later one animates. */
@Composable
fun CounterExample(
    value: Long,
    mode: CounterMode,
    duration: Duration,
) {
    KapowCounter(
        value = value,
        style =
            KapowTheme.type.numeric.copy(
                color = KapowTheme.colors.ink,
                fontSize = 48.sp,
                fontWeight = FontWeight.Medium,
            ),
        mode = mode,
        format = ::groupThousands,
        duration = duration,
    )
}

/** Groups the digits of [value] by thousands, such as 12,345. */
private fun groupThousands(value: Long): String {
    val digits = value.toString().removePrefix("-")
    val grouped =
        digits
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
    return if (value < 0) "-$grouped" else grouped
}
